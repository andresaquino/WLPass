/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dun.utils;

import com.dun.config.SshConfig;
import com.dun.file.FileUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import java.util.logging.Level;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author dubach
 */
public class FinalConfigProcessor {

    private String[] config;
    private boolean keepTargets;
    private boolean recipher;
    private File outPut;
    private int version;
    private static Logger log = Logger.getLogger(FinalConfigProcessor.class);
    /*
     * Vars del weylogic
     **/
    private static final String workingDir = System.getProperty("user.dir");
    private static final String[] jdbcOpts = {"CapacityIncrement", "DriverName", "InactiveConnectionTimeoutSeconds",
        "InitialCapacity", "MaxCapacity", "Name", "PasswordEncrypted", "Properties",
        "ShrinkFrequencySeconds", "StatementCacheSize", "Targets", "TestConnectionsOnCreate",
        "TestConnectionsOnRelease", "TestConnectionsOnReserve", "TestFrequencySeconds", "TestTableName",
        "URL"};
    private static final String[] dsOpts = {"Name","EnableTwoPhaseCommit",
        "JNDIName", "PoolName", "Targets"};
    private static final String[] dsSimpleOpts = {"Name", "JNDIName", "PoolName", "Targets"};

    public FinalConfigProcessor(String[] config, boolean keepTargets, boolean recipher, File outPut) {
        this.config = config;
        this.keepTargets = keepTargets;
        this.recipher = recipher;
        this.outPut = outPut;
        this.version = RemoteFiles.getWeblogicVersion(new File(config[0]));
        try {
            FileUtil.copy(this.config[1], workingDir + File.separatorChar + "SerializedSystemIni.dat");
        } catch (IOException ex) {
            System.exit(666);
            log.fatal("No hay Serialized que copiar... no ma... checale wey! " + ex);
        }
        startParse();
    }

    private ArrayList startParse() {
        if (version == 8) {
            System.out.println("es un 8");
            weyLogi8Parse();
        } else if (version >= 9) {
            System.out.println("es un 9 o 10");
            weyLogic910Parse();
        } else {
            System.out.println("WTF?");
        }
        return new ArrayList();
    }

    private void weyLogi8Parse() {
        XMLConfiguration config8 = new XMLConfiguration();
        try {
            config8.load(new File(config[0]));
        } catch (ConfigurationException ex) {
            log.debug(ex);
        }
        StringBuffer complete = new StringBuffer(100);
        if (config8.containsKey("JDBCConnectionPool[@Name]") && (config8.getList("JDBCConnectionPool[@Name]")).size() != 0) {
            System.out.println("multiples valores...");
            complete.append(parseMultiJDBCWL8(config8).toString());
            complete.append(parseMultiDSWL8(config8).toString());
            complete.append(parseMultiDSSimpleWL8(config8).toString());
            FileWriter fw = null;
            try {
                fw = new FileWriter(outPut);
                fw.write(complete.toString());

            } catch (IOException ex) {
                log.fatal(ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    log.fatal(ex);
                }
            }

        } else if (config8.containsKey("JDBCConnectionPool[@Name]")) {
            System.out.println("solo contiene 1 DataDource");

            FileWriter fw = null;
            try {
                fw = new FileWriter(outPut);
                fw.write(parseSinglePoolDS(config8));

            } catch (IOException ex) {
                log.fatal(ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    log.fatal(ex);
                }
            }
        }
    }

    private String parseSinglePoolDS(XMLConfiguration config) {

        StringBuffer single = new StringBuffer(2);
        String targets = "";
        if (config.containsKey("JDBCConnectionPool")) {
            single.append("<JDBCConnectionPool\n");
            for (int j = 0; j < jdbcOpts.length; j++) {
                List lista = config.getList("JDBCConnectionPool.[@" + jdbcOpts[j] + "]");
                if (lista.size() > 1) {
                    for (Object o : lista) {
                        targets += (String) o + ",";
                    }
                    targets = targets.substring(0, targets.length() - 1);
                    single.append("" + jdbcOpts[j] + "=\"" + targets + "\"\n ");
                    targets = "";
                } else if (lista.size() == 1) {
                    if (jdbcOpts[j].equals("PasswordEncrypted") && recipher) {
                        single.append("" + jdbcOpts[j] + "=\"" + recipherJDBCWL8(ClearEncryption.clear((String) lista.get(0))) + "\"\n ");
                    } else {
                        single.append("" + jdbcOpts[j] + "=\"" + ClearEncryption.clear((String) lista.get(0)) + "\"\n ");
                    }
                } else {
                    log.debug("chale aqui no paso nada :S " + jdbcOpts[j]);
                }

            }
            single.append("/>\n");
        } else if (config.containsKey("JDBCTxDataSource")) {
            single.append("<JDBCTxDataSource\n");
            for (int j = 0; j < dsOpts.length; j++) {
                List lista = config.getList("JDBCTxDataSource.[@" + dsOpts[j] + "]");
                if (lista.size() > 1) {
                    for (Object o : lista) {
                        targets += (String) o + ",";
                    }
                    targets = targets.substring(0, targets.length() - 1);
                    single.append("" + dsOpts[j] + "=\"" + targets + "\"\n ");
                    targets = "";
                } else if (lista.size() == 1) {
                    single.append("" + dsOpts[j] + "=\"" + (String) lista.get(0) + "\"\n ");
                } else {
                    log.debug("chale aqui no paso nada :S " + dsOpts[j]);
                }

            }
            single.append("/>\n");
            System.out.println(single.toString());
        } else if (config.containsKey("JDBCDataSource")) {
            single.append("<JDBCDataSource\n");
            for (int j = 0; j < dsSimpleOpts.length; j++) {
                List lista = config.getList("JDBCDataSource.[@" + dsSimpleOpts[j] + "]");
                if (lista.size() > 1) {
                    for (Object o : lista) {
                        targets += (String) o + ",";
                    }
                    targets = targets.substring(0, targets.length() - 1);
                    single.append("" + dsSimpleOpts[j] + "=\"" + targets + "\"\n ");
                    targets = "";
                } else if (lista.size() == 1) {
                    single.append("" + dsSimpleOpts[j] + "=\"" + (String) lista.get(0) + "\"\n ");
                } else {
                    log.debug("chale aqui no paso nada :S " + dsSimpleOpts[j]);
                }

            }
            single.append("/>\n");
        }
                    System.out.println(single.toString());

        return single.toString();
    }

    private StringBuffer parseMultiJDBCWL8(XMLConfiguration config) {
        StringBuffer sb = new StringBuffer(20);
        String targets = "";

        List jdbc = config.getList("JDBCConnectionPool[@Name]");
        for (int i = 0; i < jdbc.size(); i++) {
            log.debug("Processing: " + jdbc.get(i));
            sb.append("<JDBCConnectionPool ");
            for (int j = 0; j < jdbcOpts.length; j++) {
                List lista = config.getList("JDBCConnectionPool(" + i + ").[@" + jdbcOpts[j] + "]");
                if (lista.size() > 1) {
                    for (Object o : lista) {
                        targets += (String) o + ",";
                    }
                    targets = targets.substring(0, targets.length() - 1);
                    sb.append("" + jdbcOpts[j] + "=\"" + targets + "\"  ");
                    targets = "";
                } else if (lista.size() == 1) {
                    if (jdbcOpts[j].equals("PasswordEncrypted") && recipher) {
                        sb.append("" + jdbcOpts[j] + "=\"" + recipherJDBCWL8(ClearEncryption.clear((String) lista.get(0))) + "\"\n ");
                    } else {
                        sb.append("" + jdbcOpts[j] + "=\"" + ClearEncryption.clear((String) lista.get(0)) + "\"  ");
                    }
                } else {
                    log.debug("chale aqui no paso nada :S " + jdbcOpts[j]);
                }

            }
            sb.append("/>\n");
            
        }
        System.out.println(sb.toString());
        return sb;
    }

    private StringBuffer parseMultiDSWL8(XMLConfiguration config) {
        StringBuffer sb = new StringBuffer(20);
        String targets = "";

        List jdbc = config.getList("JDBCTxDataSource[@Name]");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>Imprimiendo " +jdbc.size()+"<<<<<<<<<<<<<<<<<<<<<<<<<");
        for (int i = 0; i < jdbc.size(); i++) {
            log.debug("Processing: " + jdbc.get(i));
            sb.append("<JDBCTxDataSource ");
            for (int j = 0; j < dsOpts.length; j++) {
                List lista = config.getList("JDBCTxDataSource(" + i + ").[@" + dsOpts[j] + "]");
                if (lista.size() > 1) {
                    for (Object o : lista) {
                        targets += (String) o + ",";
                    }
                    targets = targets.substring(0, targets.length() - 1);
                    sb.append("" + dsOpts[j] + "=\"" + targets + "\" ");
                    targets = "";
                } else if (lista.size() == 1) {
                    sb.append("" + dsOpts[j] + "=\"" + (String) lista.get(0) + "\" ");
                } else {
                    log.debug("chale aqui no paso nada :S " + dsOpts[j]);
                }

            }
            sb.append("/>\n");
        }
        System.out.println(sb.toString());
        return sb;
    }

    private StringBuffer parseMultiDSSimpleWL8(XMLConfiguration config) {
        StringBuffer sb = new StringBuffer(20);
        String targets = "";

        List jdbc = config.getList("JDBCDataSource[@Name]");
        System.out.println(">>>>>>>>>>>>>>>>>>>>>Imprimiendo " +jdbc.size()+"<<<<<<<<<<<<<<<<<<<<<<<<<");
        for (int i = 0; i < jdbc.size(); i++) {
            log.debug("Processing: " + jdbc.get(i));
            sb.append("<JDBCDataSource ");
            for (int j = 0; j < dsSimpleOpts.length; j++) {
                List lista = config.getList("JDBCDataSource(" + i + ").[@" + dsSimpleOpts[j] + "]");
                if (lista.size() > 1) {
                    for (Object o : lista) {
                        targets += (String) o + ",";
                    }
                    targets = targets.substring(0, targets.length() - 1);
                    sb.append("" + dsSimpleOpts[j] + "=\"" + targets + "\" ");
                    targets = "";
                } else if (lista.size() == 1) {
                    sb.append("" + dsSimpleOpts[j] + "=\"" + (String) lista.get(0) + "\" ");
                } else {
                    log.debug("chale aqui no paso nada :S " + dsSimpleOpts[j]);
                }

            }
            sb.append("/>\n");
        }
        System.out.println(sb.toString());
        return sb;
    }

    private ArrayList<String> decipherJDBCWL8(XMLConfiguration config) {
        ArrayList<String> descifrados = new ArrayList<String>(10);

        try {
            FileUtil.copy(this.config[1], workingDir + File.separatorChar + "SerializedSystemIni.dat");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FinalConfigProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        List jdbc = config.getList("JDBCConnectionPool[@PasswordEncrypted]");
        for (int i = 0; i < jdbc.size(); i++) {
            descifrados.add(config.getString("JDBCDataSource(" + i + ").[@Name]") + "->" + ClearEncryption.clear(config.getString("JDBCDataSource(" + i + ").[@PasswordEncrypted]")));
        }
        return descifrados;
    }

    private String recipherJDBCWL8(String pass) {
        String cifrados = "";

        try {
            FileUtil.copy(SshConfig.getTempDir() + File.separator + "new" + File.separator + "SerializedSystemIni.dat", workingDir + File.separatorChar + "SerializedSystemIni.dat");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FinalConfigProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        cifrados = ClearEncryption.encryptPassword(pass);
        try {
            FileUtil.copy(this.config[1], workingDir + File.separatorChar + "SerializedSystemIni.dat");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FinalConfigProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cifrados;
    }

    private void weyLogic910Parse() {
        XMLConfiguration config9 = new XMLConfiguration();
        try {
            config9.load(new File(config[0]));
        } catch (ConfigurationException ex) {
            log.debug(ex);
        }
        File jdbcDir = new File(workingDir + File.separator + "jdbc");
        File[] toDelete = null;
        if (!jdbcDir.exists()) {
            jdbcDir.mkdir();
        } else {
            toDelete = jdbcDir.listFiles();
            for (int i = 0; i < toDelete.length; i++) {
                if (toDelete[i].delete()) {
                    System.out.println(toDelete[i].toString() + " borrado satisfactoriamente");
                } else {
                    System.out.println("pedos al borrar " + toDelete[i].toString());
                }
            }
        }
        startWeyLogic9Parse(config9, jdbcDir);

    }

    private void startWeyLogic9Parse(XMLConfiguration config, File jdbcDir) {
        if(! new File(outPut.toString() + "testing.this.shit").exists()){

        }
        XMLConfiguration configOut = null;
        try {
            configOut = new XMLConfiguration(new File(outPut.toString() + "testing.this.shit"));
        } catch (ConfigurationException ex) {
            java.util.logging.Logger.getLogger(FinalConfigProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        List obj = config.configurationsAt("jdbc-system-resource");
        Iterator it = config.getKeys("jdbc-system-resource");
        ArrayList<File> descriptors = new ArrayList<File>(1);
        ArrayList<String> configJDBC = new ArrayList<String>(3);
        ArrayList<String> jdbcKey = new ArrayList<String>(1);

        while (it.hasNext()) {
            String tempKey = it.next().toString();
            jdbcKey.add(
                    tempKey.substring(tempKey.lastIndexOf(".") + 1, tempKey.length()));
        }
        if (obj instanceof Collection) {
            System.out.println("son " + ((Collection) obj).size() + " keys de configuracion");
        } else {
            System.out.println(config.getString("jdbc-system-resource.name"));
        }
        
        for (Object sc : obj) {
            SubnodeConfiguration sub = (SubnodeConfiguration) sc;
            for (int i = 0; i < jdbcKey.size(); i++) {
                configJDBC.add(jdbcKey.get(i) + ":" + sub.getString(jdbcKey.get(i)));
            }
        }
        int y = 0;
        for (int i = 0; i < configJDBC.size(); i++) {
            if (i % 3 == 0 && i > 0) {
                System.out.println("_________________________________________");
                y++;
            }

            String key = configJDBC.get(i).substring(0, configJDBC.get(i).lastIndexOf(":"));
            String value = configJDBC.get(i).substring(configJDBC.get(i).lastIndexOf(":") + 1, configJDBC.get(i).length());
            if (key.equals("descriptor-file-name")) {
                descriptors.add(new File(config.getFile().getParent() +
                        File.separatorChar + value));
            }
            configOut.setProperty("jdbc-system-resource(" + y + ")." + key, value);
            System.out.println(key + " => " + value);

        }
        try {
            configOut.save(outPut);
        } catch (ConfigurationException ex) {
        }
        StringBuffer web9Passwords = new StringBuffer(40);

        for (File f : descriptors) {
            if (f.exists()) {
                System.out.println("Procesando " + f.getName());
                String encrypted = "", decrypted = "";
                XMLConfiguration descriptorconfig = new XMLConfiguration();
                try {
                    FileUtil.copy(SshConfig.getTempDir() + File.separator +
                            "jdbc" + File.separator + f.getName(), jdbcDir.toString() +
                            File.separator + f.getName());

                } catch (IOException ex) {
                }
                try {
                    descriptorconfig.load(new File(jdbcDir.toString() + File.separator + f.getName()));
                } catch (ConfigurationException ex) {
                }
                String pass = descriptorconfig.getString("jdbc-driver-params.password-encrypted");
                if(pass != null ){
                    decrypted = ClearEncryption.clear(pass);
                }else{
                    decrypted = "no me lo se";
                }
                if(recipher){

                    descriptorconfig.setProperty("jdbc-driver-params.password-encrypted", recipherJDBCWL8(decrypted));
                }else{
                    descriptorconfig.setProperty("jdbc-driver-params.password-encrypted", decrypted);
                }
//                descriptorconfig.setProperty("jdbc-driver-params.password-encrypted", " ");

                web9Passwords.append(f.getName() + ":" + decrypted + "\n");

                try {
                    descriptorconfig.save(jdbcDir.toString() +
                            File.separator + f.getName() + ".emp");
                } catch (ConfigurationException ex) {
                }

                System.out.println(f.toString());
            } else {
                System.out.println(" :( ");
            }
        }
    }
}
