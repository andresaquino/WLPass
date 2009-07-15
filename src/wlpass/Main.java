/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wlpass;

import com.dun.file.FileUtil;
import com.dun.parser.WLShellJDBCParser;
import com.dun.utils.ClearEncryption;
import com.dun.utils.DomainCrawler;
import com.dun.utils.FinalConfigProcessor;
import com.dun.utils.RemoteFiles;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author dubach
 */
public class Main {

    @SuppressWarnings("static-access")
    static Options genTypeOptions() {
        Options opt = new Options();
        //estas son las opciones que no deben de cambiar
        opt.addOption("k", false, "Keep targets after extract.");
        opt.addOption("r", false, "Encrypt all the extracted passwords.");
        opt.addOption("v", false, "Print version and exit");
        opt.addOption("version", false, "Prints version and exit.");
        opt.addOption(OptionBuilder.withArgName("type").hasArg().isRequired().withDescription("Type of action to do extract | single.").create("t"));

        opt.addOption(OptionBuilder.withArgName("input_file").hasArg().isRequired().withDescription("Input file local or remote with user@host:/path/to/DOMAIN_HOME" +
                " syntax this shit is smart enough to fetch the correct files.").create("i"));

        opt.addOption(OptionBuilder.withArgName("serialized").hasArg().withDescription("Local or remote NEW serialized for re-encryption" +
                " if its remote specify just the domain home.").create("sn"));

        opt.addOption(OptionBuilder.withArgName("output_file").hasArg().withDescription("File where all the parsed config will end up.").create("o"));


        return opt;
    }
    private static final String workingDir = System.getProperty("user.dir");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        // create the parser
        CommandLineParser parser = new GnuParser();
        Options options = genTypeOptions();
        try {
            boolean keepTargets = false, recrypt = false;
            String[] contenedor = null;

            // parse the command line arguments
            CommandLine line = parser.parse(options, args, true);

            String type = line.getOptionValue("t");
            String input = line.getOptionValue("i");
            keepTargets = line.hasOption("k");
            String sn = line.getOptionValue("sn");
            String output = line.getOptionValue("o");
            recrypt = line.hasOption("r");
            if (type.equalsIgnoreCase("single") && input.contains("@")) {
                contenedor = RemoteFiles.getRemoteFiles(input, type);
                try {
                    FileUtil.copy(contenedor[1], workingDir + File.separatorChar + "SerializedSystemIni.dat");
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                Scanner sc = null;
                try {
                    sc = new Scanner(new File(contenedor[2]));
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
                while (sc.hasNext()) {
                    String temp = sc.nextLine();
                    if (temp.contains("{")) {
                        if (temp.contains("\\=")) {
                            temp = temp.replace("\\=", "=");
                        }
                        System.out.println(ClearEncryption.clear(temp.substring(temp.indexOf("=") + 1)));
                    }
                }
                RemoteFiles.deleteTempFiles();
                System.exit(0);
            }
            if(type.equals("crawl")){
                DomainCrawler.main(new String[] {input});
                System.exit(0);
            }
            if(type.equals("parse")){
                WLShellJDBCParser.main(args);
            }
            if (type.equalsIgnoreCase("single") && !input.contains("@")) {
            }
            if (recrypt && !line.hasOption("sn")) {
                System.err.println("OMFG como quieres recifrar sin una nueva semilla? necesitas especificar -sn <serialized>");
                System.exit(1);
            }
            //validamos las rutas si son locales o remotas
            if (input.contains("@")) {
//                System.out.println("getting files: " + input + " - " + type);
                contenedor = RemoteFiles.getRemoteFiles(input, type);
            } else {
                System.out.println("no mas checamos que exista");
            }

            if (line.hasOption("sn") && recrypt) {
                if (sn.contains("@")) {
                    //contenedor = RemoteFiles.getRemoteFiles(input, type);
                    sn = RemoteFiles.getNewSerialized(sn);
                } else {
                    System.out.println("checamos que exista");
                }
            }
            System.out.println("al parecer ya estan todos los pinches configs y demas archivos... ");
            FinalConfigProcessor fcp = new FinalConfigProcessor(contenedor, keepTargets, recrypt, new File(output));
        } catch (ParseException exp) {
            boolean flag = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-v") || args[i].startsWith("-V")) {
                    flag = true;
                }
            }
            // oops, something went wrong
            if (flag) {
                System.out.println("Version: 0.99-RC1");
            } else {
                System.err.println("Parsing failed.  Reason: " + exp.getMessage());
                System.out.println(exp.getMessage());
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("WLPass -t <type> -i <input_file> -s <serialized> -o" +
                        " <output_file> [ -k -r ] [ -sn <new serialized]", options);
            }
        } finally {
            RemoteFiles.deleteTempFiles();
        }

    }
}
