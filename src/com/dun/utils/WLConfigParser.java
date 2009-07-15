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
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author dubach
 */
public class WLConfigParser {

    private static final String workingDir = System.getProperty("user.dir");
    private static final String[] jdbcOpts = {"CapacityIncrement", "DriverName", "InactiveConnectionTimeoutSeconds",
        "InitialCapacity", "MaxCapacity"/*,"Name"*/, "PasswordEncrypted", "Properties",
        "ShrinkFrequencySeconds", "StatementCacheSize", "Targets", "TestConnectionsOnCreate",
        "TestConnectionsOnRelease", "TestConnectionsOnReserve", "TestFrequencySeconds", "TestTableName",
        "URL"};
    private static final String[] dsOpts = {"EnableTwoPhaseCommit",
        "JNDIName", "PoolName", "Targets"};
    private static final String[] dsSimpleOpts = {"JNDIName", "PoolName", "Targets"};

    public static void extract8(String[] contenedor, String sn, String outFile, boolean recrypt, boolean keep) {
        /**
         * El contenedor quedara asi
         * [0] = config.xml
         * [1] = Serialized
         * [2] = boot.properties
         */
        File out = null;
        if (!outFile.equals("")) {
            out = new File(outFile);
        }
        File in = new File(contenedor[0]);
        File newSer = null;
        if (recrypt && !sn.isEmpty()) {
            newSer = new File("/tmp/new/SerializedSystemIni.dat");
        }


        StringBuffer passwords = new StringBuffer(10);
        XMLConfiguration config = new XMLConfiguration();
        StringBuffer sbJDBC = new StringBuffer(20);
        StringBuffer sbDS = new StringBuffer(20);

        try {
            config.load(in);
        } catch (ConfigurationException ex) {
            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        Object obj = config.getProperty("JDBCConnectionPool[@Name]");
        int cont = 0;
        String temp = "";
        System.out.println("obj size: " + ((Collection) obj).size());
        try {
            FileUtil.copy(SshConfig.getTempDir() + File.separator + "SerializedSystemIni.dat",
                    workingDir + File.separator + "SerializedSystemIni.dat");
        } catch (IOException ex) {
            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (obj instanceof Collection) {

            Collection collection = (Collection) obj;
            for (int j = 0; j < ((Collection) obj).size(); j++) {
                temp = config.getString("JDBCConnectionPool(" + j + ").[@Name]");
                System.out.println("<JDBCConnectionPool Name=\"" + temp + "\"");
                sbJDBC.append("<JDBCConnectionPool Name=\"" + temp.replace(" ", "") + "\"\n");
                for (int i = 0; i < jdbcOpts.length; i++) {
                    String value = config.getString("JDBCConnectionPool(" + j + ").[@" + jdbcOpts[i] + "]");
                    String cleared = "";
                    if (value == null) {
                        value = "";
                    }
                    if ((jdbcOpts[i].equals("Targets") || jdbcOpts[i].equals("Target")) && keep) {
                        value = "";
                        System.out.println("Los targets son... " + value);
                        List lista = config.getList("JDBCConnectionPool(" + j + ").[@" + jdbcOpts[i] + "]");
                        if (lista != null || !lista.isEmpty()) {
                            for (Object o : lista) {
                                System.out.println((String) o);
                                value += (String) o + ",";
                            }
                            if (value.endsWith(",")) {
                                value = value.substring(0, value.length() - 1);
                            }
                        } else {
                            value = config.getString("JDBCConnectionPool(" + j + ").[@" + jdbcOpts[i] + "]");
                        }
                    } else {
                        value = "";
                    }
//                    if (jdbcOpts[i].equals("Targets")) {
//                        value = "";
//                    }
                    if (jdbcOpts[i].equals("PasswordEncrypted")) {
                        value = "";
                        try {
                            FileUtil.copy(SshConfig.getTempDir() + File.separator + "SerializedSystemIni.dat",
                                    workingDir + File.separator + "SerializedSystemIni.dat");
                        } catch (IOException ex) {
                            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        passwords.append(temp + ":" + ClearEncryption.clear(config.getString("JDBCConnectionPool(" + j + ").[@" + jdbcOpts[i] + "]")) + "\n");
                        cleared = ClearEncryption.clear(config.getString("JDBCConnectionPool(" + j + ").[@" + jdbcOpts[i] + "]"));
                    }
                    if (recrypt == true && !cleared.equals("")) {
                        try {
                            FileUtil.copy(SshConfig.getTempDir() + File.separator + "new" + File.separator + "SerializedSystemIni.dat",
                                    workingDir + File.separator + "SerializedSystemIni.dat");
                        } catch (IOException ex) {
                            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        sbJDBC.append("" + jdbcOpts[i] + "=\"" + ClearEncryption.encryptPassword(cleared) + "\"\n ");
                    } else {
                        sbJDBC.append("" + jdbcOpts[i] + "=\"" + value + "\"\n ");
                    }
                }
                sbJDBC.append("/>\n");
            }
        }
        //Termina CONNECTION POOL
        obj = config.getProperty("JDBCTxDataSource[@Name]");
        cont = 0;
        temp = "";

        if (obj instanceof Collection) {
            System.out.println("obj size: " + ((Collection) obj).size());
            for (int j = 0; j < ((Collection) obj).size(); j++) {
                temp = config.getString("JDBCTxDataSource(" + j + ").[@Name]");
                System.out.println("<JDBCTxDataSource Name=\"" + temp + "\"");
                sbDS.append("<JDBCTxDataSource Name=\"" + temp + "\"\n");
                for (int i = 0; i < dsOpts.length; i++) {
                    String value = config.getString("JDBCTxDataSource(" + j + ").[@" + dsOpts[i] + "]");
                    if (value == null) {
                        value = "";
                    }
                    if ((dsOpts[i].equals("Targets") || dsOpts[i].equals("Target")) && keep) {
                        value = "";
                        System.out.println("Los targets son... " + value);
                        List lista = config.getList("JDBCTxDataSource(" + j + ").[@" + dsOpts[i] + "]");
                        if (lista != null || !lista.isEmpty()) {
                            for (Object o : lista) {
                                System.out.println((String) o);
                                value += (String) o + ",";
                            }
                            if (value.endsWith(",")) {
                                value = value.substring(0, value.length() - 1);
                            }
                        } else {
                            value = config.getString("JDBCTxDataSource(" + j + ").[@" + dsOpts[i] + "]");
                        }
                    } else {
                        value = "";
                    }
//                    if (dsOpts[i].equals("Targets") || dsOpts[i].equals("Target")) {
//                        value = "";
//                    }
                    if (dsOpts[i].equals("PoolName")) {
                        value = value.replace(" ", "");
                    }
                    sbDS.append("" + dsOpts[i] + "=\"" + value + "\" \n");
                }
                sbDS.append("/>\n");
            }
        }
        obj = config.getProperty("JDBCDataSource[@Name]");
        cont = 0;


        if (obj instanceof Collection) {
            System.out.println("obj size: " + ((Collection) obj).size());
            for (int j = 0; j < ((Collection) obj).size(); j++) {
                temp = config.getString("JDBCDataSource(" + j + ").[@Name]");
                System.out.println("<JDBCDataSource Name=\"" + temp + "\"");
                sbDS.append("<JDBCDataSource Name=\"" + temp + "\"\n");
                for (int i = 0; i < dsSimpleOpts.length; i++) {
                    String value = config.getString("JDBCDataSource(" + j + ").[@" + dsSimpleOpts[i] + "]");
                    if (value == null) {
                        value = "";
                    }
                    if ((dsSimpleOpts[i].equals("Targets") || dsSimpleOpts[i].equals("Target")) && keep) {
                        value = "";
                        System.out.println("Los targets son... " + value);
                        List lista = config.getList("JDBCTDataSource(" + i + ").[@" + dsSimpleOpts[i] + "]");
                        if (lista != null || !lista.isEmpty()) {
                            for (Object o : lista) {
                                System.out.println((String) o);
                                value += (String) o + ",";
                            }
                            if (value.endsWith(",")) {
                                value = value.substring(0, value.length() - 1);
                            }
                        } else {
                            value = config.getString("JDBCTxDataSource(" + j + ").[@" + dsOpts[i] + "]");
                        }
                    } else {
                        value = "";
                    }
//                    if (dsOpts[i].equals("Targets") || dsOpts[i].equals("Target")) {
//                        value = "";
//                    }
                    if (dsSimpleOpts[i].equals("PoolName")) {
                        value = value.replace(" ", "");
                    }
                    sbDS.append("" + dsSimpleOpts[i] + "=\"" + value + "\" \n");
                }
                sbDS.append("/>\n");
            }
        }
        if (out == null) {
            FileWriter fw = null;
            try {
                fw = new FileWriter(out);
                fw.write(sbJDBC.toString());
                fw.append(sbDS.toString());
                fw.append(passwords.toString());
            } catch (IOException ex) {
                Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            System.out.println(sbJDBC.toString());
            System.out.println(sbDS.toString());
            System.out.println(passwords.toString());
        }
    }

    public static ArrayList<File> extract9(String[] contenedor, String sn, String outFile, boolean recrypt, boolean keep) {
        File out = null;
        if (!outFile.equals("")) {
            out = new File(outFile);
        }
        File in = new File(contenedor[0]);
        File newSer = null;
        if (recrypt && !sn.isEmpty()) {
            newSer = new File("/tmp/new/SerializedSystemIni.dat");
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
        XMLConfiguration config = new XMLConfiguration();
        XMLConfiguration configOut = new XMLConfiguration();
        ArrayList<File> descriptors = new ArrayList<File>(1);
        ArrayList<String> configJDBC = new ArrayList<String>(3);
        ArrayList<String> jdbcKey = new ArrayList<String>(1);
        try {
            FileUtil.copy(SshConfig.getTempDir() + File.separator + "SerializedSystemIni.dat", workingDir + File.separator + "SerializedSystemIni.dat");
        } catch (IOException ex) {
            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            config.load(in);
        } catch (ConfigurationException ex) {
            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        List obj = config.configurationsAt("jdbc-system-resource");
        Iterator it = config.getKeys("jdbc-system-resource");

        while (it.hasNext()) {
            String tempKey = it.next().toString();
            jdbcKey.add(
                    tempKey.substring(tempKey.lastIndexOf(".") + 1, tempKey.length()));
        }
        if (obj instanceof Collection) {
            System.out.println(((Collection) obj).size());
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
                descriptors.add(new File(in.getParentFile().toString() +
                        File.separatorChar + value));
            }
            configOut.setProperty("jdbc-system-resource(" + y + ")." + key, value);
            System.out.println(key + " => " + value);
        }
        try {
            configOut.save(out);
        } catch (ConfigurationException ex) {
            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    descriptorconfig.load(new File(jdbcDir.toString() + File.separator + f.getName()));
                } catch (ConfigurationException ex) {
                    Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
                }

                decrypted = ClearEncryption.clear(descriptorconfig.getString("jdbc-driver-params.password-encrypted"));
                descriptorconfig.setProperty("jdbc-driver-params.password-encrypted", " ");

                web9Passwords.append(f.getName() + ":" + decrypted + "\n");

                try {
                    descriptorconfig.save(jdbcDir.toString() +
                            File.separator + f.getName() + ".emp");
                } catch (ConfigurationException ex) {
                    Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.println(f.toString());
            } else {
                System.out.println(" :( ");
            }
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(out.toString() + ".pass"));
            fw.write(web9Passwords.toString());
            fw.flush();
        } catch (IOException ex) {
            Logger.getLogger(WLConfigParser.class.getName()).log(Level.SEVERE, null, ex);
        }

        return descriptors;
    }
}