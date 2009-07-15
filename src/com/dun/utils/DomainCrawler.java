/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dun.utils;

import com.dun.config.SshConfig;
import com.dun.file.FileUtil;
import com.dun.parser.WLShellJDBCParser;
import com.dun.ssh.SshCon;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author dubach
 */
public class DomainCrawler {

    private static Logger log = Logger.getLogger(RemoteFiles.class);
    private static String tempDir = SshConfig.getTempDir();
    private String user;
    private String host;
    private static final String workingDir = System.getProperty("user.dir");

    public DomainCrawler(String string_conexion) {
        this.user = string_conexion.substring(0, string_conexion.lastIndexOf("@"));
        this.host = string_conexion.substring(string_conexion.lastIndexOf("@") + 1, string_conexion.length());
    }

    private ArrayList<String> /*void*/ crawlForDomains() {
        ArrayList<String> domainsFound = new ArrayList<String>(1);
        SshCon ssh = new SshCon(user, host);
        ssh.openConnection("pwd", true);
        //hay unos servidores chaquetos que no tienen el home en WEYLOGIC_RUTH, comprobamos eso
        log.debug("hay unos servidores que no cumplen con el $HOME estructurado tal que $HOME/bea no existe, comprobamos eso");
        String homeDir = ssh.getStdOut();
        ssh.flushStdOut();
        if (homeDir.startsWith("/home")) {
            //el standard de nextel es: /opt/<unix_user>/bea/blah
            homeDir = homeDir.replace("/home", "/opt");
        }
        //Teniendo el home correcto, "virigüamos" que version tiene
        ssh.execSingleCommand("ls " + homeDir + "/bea");
        String webInstalled = ssh.getStdOut();
        log.debug("Obteniendo la version del weylogic: " + webInstalled);
        ssh.flushStdOut();
        //ahora buscamos todos los configs...
        ssh.execSingleCommand(" find /opt -name  \"config.xml\" -user " + user);
        String[] findResult = ssh.getStdOut().split("\n");

        for (int i = 0; i < findResult.length; i++) {
            if (!findResult[i].contains("weblogic81")) {
                domainsFound.add(findResult[i].substring(0, findResult[i].lastIndexOf("/")));
            }

        }
        return domainsFound;
    }

    public static void main(String args[]) {

        DomainCrawler dc = new DomainCrawler(args[0]);//supongamos que este es el
        ArrayList<String> domains = dc.crawlForDomains();
        String[] contenedor = null;
        String ip = "-s="+args[0].substring(args[0].lastIndexOf("@") +1, args[0].length());
        String listenPort = "";
        String user = "";
        String password = "";
        for (String s : domains) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(DomainCrawler.class.getName()).log(Level.SEVERE, null, ex);
            }

            String conexion = args[0] + ":" + s;

            contenedor = RemoteFiles.getRemoteFiles(conexion, "");
            try {
                FileUtil.copy(contenedor[1], workingDir + File.separatorChar + "SerializedSystemIni.dat");
            } catch (IOException ex) {
                log.debug(ex);
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
                    if (temp.startsWith("username")) {
                        user = "-u=" + ClearEncryption.clear(temp.substring(temp.indexOf("=") + 1));
                    }
                    if (temp.startsWith("password")) {
                        password = "-pw=" + ClearEncryption.clear(temp.substring(temp.indexOf("=") + 1));
                    }

                }
            }
            //Obtenemos la version de Weblogic pa sacar el listenport
            File config = new File(contenedor[0]);
            int version = RemoteFiles.getWeblogicVersion(config);

            if (version == 8) {
                XMLConfiguration config8 = new XMLConfiguration();
                try {
                    config8.load(config);
                } catch (ConfigurationException ex) {
                    log.info(ex);
                }
                listenPort = "-p=" + config8.getString("Server.[@ListenPort]");
            }
            if (version == 9) {
            }
            String[] argumentos = new String[5];
            argumentos[0] = System.getProperty("user.home")+"/scripts/wls_pool_test.sh";
            argumentos[1] = user;
            argumentos[2] = password;
            argumentos[3] = listenPort;
            argumentos[4] = ip;
            try {
                System.out.println("");
                Runtime.getRuntime().exec(argumentos);

            } catch (IOException ex) {
                log.info(ex);
            }
            RemoteFiles.deleteTempFiles();
        }
        System.out.println(WLShellJDBCParser.parseJDBC());

    }
}
