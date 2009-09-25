/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dun.utils;

import com.dun.config.SshConfig;
import com.dun.ssh.SshCon;
import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author dubach
 */
public class RemoteFiles {

    private static Logger log = Logger.getLogger(RemoteFiles.class);
    private static String tempDir = SshConfig.getTempDir();

    public static String[] getRemoteFiles(String remoteFile, String type) {

        String[] contenedor = new String[4];
        /**
         * El contenedor quedara asi
         * [0] = config.xml
         * [1] = Serialized
         * [2] = boot.properties
         * Si es web9 | web10
         * [0] config.xml
         * [1] Serialized
         * [2] boot.properties
         * [3] lista de archivos con el formato: file1|file2|file3|... 
         * para ser spliteados despues
         */
        //parseamos la cadena para obtener los valores de conexion
        String ruser = remoteFile.substring(0, remoteFile.lastIndexOf("@"));
        String raddress = remoteFile.substring(remoteFile.lastIndexOf("@") + 1, remoteFile.indexOf(":"));
        String rfile = remoteFile.substring(remoteFile.lastIndexOf(":") + 1, remoteFile.length());

        //creamos la conexion
        log.info("Creando la conexion; " + ruser + "@" + raddress);
        SshCon con = new SshCon(ruser, raddress);

        con.openConnection("hostname", true);
        con.flushStdOut();
        //se usa para obtener la version de weylogic

        //si el path empieza con / no es relativo, y se usara el path completo hasta bea
        if(rfile.startsWith("/")){
            System.out.println("shiaaaaaaaales "+ rfile);
            System.out.println(rfile.substring(0,rfile.lastIndexOf("bea")));
            con.execSingleCommand("ls "+ rfile.substring(0,rfile.lastIndexOf("bea")) + "bea");
        }else{
            con.execSingleCommand("ls bea");
        }
        con.execSingleCommand("ls bea");

        String webInstalled = con.getStdOut();
        log.debug("Obteniendo la version del weylogic: " + webInstalled);
        con.flushStdOut();
        //no recuerdo para que lo queria... pero ya vere si lo quito o nelson
        //TODO validar esta mamada
        con.execSingleCommand("echo $HOME");

        String path = rfile + "/";
        log.info(path);
        con.flushStdOut();

        //en web8 todo esta en el config

        if (webInstalled.contains("weblogic81")) {
            log.info("es un wey8");
            log.info("get " + path + "SerializedSystemIni.dat a " + tempDir);
            con.scpClient("get", path + "SerializedSystemIni.dat a ", tempDir);
            log.info("get " + path + "config.xmla " + tempDir);
            con.scpClient("get", path + "config.xml", tempDir);
            log.info("get " + path + "boot.properties a " + tempDir);
            con.scpClient("get", path + "boot.properties", tempDir);
            contenedor[0] = tempDir + File.separator + "config.xml";
            contenedor[1] = tempDir + File.separator + "SerializedSystemIni.dat";
            contenedor[2] = tempDir + File.separator + "boot.properties";
        }

        if (webInstalled.contains("weblogic92") || webInstalled.contains("wlserver_10.3")) {
            log.info("es un wey de 9 a 10");

            log.info("get " + path + "config/config.xml a " + tempDir);
            con.scpClient("get", path + "config/config.xml", tempDir);
            contenedor[0] = tempDir + File.separator + "config.xml";
            con.resetStdOut();
            con.execSingleCommand("find $HOME/"+rfile+" -name SerializedSystemIni.dat | tail -1");
            String ser = con.flushStdOut();
//            log.info("get " + path + "security/SerializedSystemIni.dat a " + tempDir);
            log.info("get " + ser+" a " + tempDir);
            con.scpClient("get", ser, tempDir);
            contenedor[1] = tempDir + File.separator + "SerializedSystemIni.dat";

            log.info("get " + path + "boot.properties a " + tempDir);
            con.scpClient("get", path + "boot.properties", tempDir);
            contenedor[2] = tempDir + File.separator + "boot.properties";

            File temporal = new File(SshConfig.getTempDir() + "/jdbc");
            if (!temporal.exists()) {
                temporal.mkdirs();
            }
            //cuando es web9+ se tiene que bajar la carpeta jdbc en "tmp"/jdbc/*
            //y el config en "tmp"/config.xml
            path += "config/jdbc/";

            con.execSingleCommand("ls " + path + " | grep xml");
            String fileList = con.getStdOut();
            String[] listado = fileList.split("\n");
            contenedor[3] = fileList.replace("\n", "|");

            for (int i = 0; i < listado.length; i++) {
                log.info("Agregando... " + path + listado[i]);
                listado[i] = path + listado[i];

            }
            con.scpRecursiveClient("get", listado, temporal.toString());
        }
        con.close();
        return contenedor;
    }

    public static String getNewSerialized(String remoteFile) {
        String bingo = "";
        String ruser = remoteFile.substring(0, remoteFile.lastIndexOf("@"));
        String raddress = remoteFile.substring(remoteFile.lastIndexOf("@") + 1, remoteFile.indexOf(":"));
        String rfile = remoteFile.substring(remoteFile.lastIndexOf(":") + 1, remoteFile.length());

        //creamos la conexion
        log.info("Creando la conexion; " + ruser + "@" + raddress + " para recifrar");
        SshCon con = new SshCon(ruser, raddress);
        con.openConnection("hostname", true);
        con.flushStdOut();
        //se usa para obtener la version de weylogic
        con.execSingleCommand("ls bea");
        String webInstalled = con.getStdOut();
        con.flushStdOut();
        //no recuerdo para que lo queria... pero ya vere si lo quito o nelson
        //TODO validar esta mamada
        con.execSingleCommand("echo $HOME");

        String path = rfile + "/" ;
        con.flushStdOut();
        File temporal = new File(SshConfig.getTempDir() + "/new");
        if (!temporal.exists()) {
            temporal.mkdirs();
        }
        if (webInstalled.contains("weblogic81")) {
            log.info("Obteniendo el archivo " + path + "SerializedSystemIni.dat a " + temporal.toString());
            con.scpClient("get", path + "SerializedSystemIni.dat", temporal.toString());
            bingo = tempDir + File.separator + "SerializedSystemIni.dat";
        }

        if (webInstalled.contains("weblogic92") || webInstalled.contains("wlserver_10.3")) {
            log.info("Obteniendo el archivo " + path + "security/SerializedSystemIni.dat a " + temporal.toString());
            con.scpClient("get", path + "security/SerializedSystemIni.dat", temporal.toString());
            bingo = tempDir + File.pathSeparator + "SerializedSystemIni.dat";
        }
        return bingo;
    }

    public static int getWeblogicVersion(File in) {
        int version = 0;
        XMLConfiguration config = new XMLConfiguration();
        try {
            config.load(in);
        } catch (ConfigurationException ex) {
            log.fatal("Failed to load configuration", ex);
        }

        String version8 = config.getString("Server[@ServerVersion]");
        String version9 = config.getString("domain-version");
        if (version8 != null) {
            version = 8;
        }
        if (version9 != null) {
            version = 9;
        }
        return version;
    }

    public static boolean isStructured(File in) {
        File jdbc = new File(in.getParentFile().toString() + File.separatorChar + "jdbc");
        return (jdbc.exists() && jdbc.listFiles().length > 0) ? true : false;
    }
    public static void deleteTempFiles(){
        File ser = new File("/tmp/SerializedSystemIni.dat");
        File boot = new File("/tmp/boot.properties");
        File jdbc = new File("/tmp/jdbc");
        if(ser.exists()){
            ser.delete();
        }
        if(boot.exists()){
            boot.delete();
        }
        if(jdbc.exists() && jdbc.isDirectory()){
            File[] dirList = jdbc.listFiles();
            for (int i =0;i < dirList.length;i++){
                dirList[i].delete();
            }
            jdbc.delete();
            log.info("Temp files deleted");
        }
    }
}
