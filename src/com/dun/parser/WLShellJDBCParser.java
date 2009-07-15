/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dun.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dubach
 */
public class WLShellJDBCParser {
//header: wlsh [not connected]> connecting to t3://10.103.11.4:16000 as admin...done
//- the server name is "wlinoeUAT" and belongs to "wliNOEuat" domain
//luego: variable pools set to [bpmArchPool, cgJMSPool-nonXA, cgPool] (java.util.TreeSet)
//DVNSNOE_VSN tiene los targets [wliNOEuat:Name=clusNOE1,Type=Cluster]
//Luego:variable datasources set to
//[ImageDS, MAUseguridadDS, NOECapturavisionDS, NOEMiscDS, PDVDS, SCVbscsDS, SCVmibasDS, SCVvisionDS, bscsDS, mibasDS, miscDS, ofnxDS, vision2DS, visionDS, visionSOEDS]
//los targets de ImageDS son [wliNOEuat:Name=clusNOE1,Type=Cluster]
//variable datasources set to [NoeOCCInexindeDS, bpmArchDataSource, cgDataSource, cgDataSource-nonXA] (java.util.TreeSet)
//los targets de NoeOCCInexindeDS son [wliNOEuat:Name=clusNOE1,Type=Cluster]

    public static String parseJDBC() {
        Scanner sc = null;
        String[] hola = null;
        try {
            sc = new Scanner(new File(System.getProperty("user.home") + "/scripts/wls_pool_test.sh"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WLShellJDBCParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        String WL_SHELL = "";
        System.out.println("Entrando al Parseo");
        while (sc.hasNext()) {
            String temporal = sc.nextLine();
            if (temporal.startsWith("WLSHELL_PATH")) {
                WL_SHELL = temporal.substring(temporal.lastIndexOf("=") + 2, temporal.length() - 1);
                System.out.println("Creo que es esta " + WL_SHELL);
                if (WL_SHELL.startsWith("${HOME}")) {
                    WL_SHELL = WL_SHELL.replace("${HOME}", System.getProperty("user.home") + File.separator);
                }
                if (WL_SHELL.startsWith("~/")) {
                    WL_SHELL = WL_SHELL.replace("~/", System.getProperty("user.home") + File.separator);
                }
            }
        }
        System.out.println("La ruta de WLSHELL " + WL_SHELL);
        File[] lista = new File(WL_SHELL + File.separator + "scripts" + File.separator + "logs").listFiles();
        System.out.println("Tenemos " + lista.length + " logs");
        for (int i = 0; i < lista.length; i++) {
            System.out.println("__________START " + lista[i].getName() + "_______________");
            try {
                sc = new Scanner(lista[i]);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(WLShellJDBCParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            String domain = "";
            String server = "";
            ArrayList<String> pooles = new ArrayList<String>(10);
            ArrayList<String> status = new ArrayList<String>(10);
            //Sacamos el nombre del dominio
            while (sc.hasNext()) {
                String temporal = sc.nextLine();
                if (temporal.contains("server name")) {
                    hola = temporal.split("\"");
                    server = hola[1];
                    domain = hola[3];
                    hola = null;
                }
                if (temporal.contains("variable pools set to") && !temporal.contains("false") && !temporal.contains("true")) {
                    System.out.println("Pooles del dominio " + domain + " servidor " + server + " " + temporal.substring(temporal.lastIndexOf("["), temporal.lastIndexOf("]")));
                    pooles.add(temporal.substring(temporal.lastIndexOf("["), temporal.lastIndexOf("]")));
                }
                if (temporal.startsWith(":")) {
                    hola = temporal.split(":");
                    System.out.println("");
                    for (int j = 0; j < hola.length; j++) {
                        System.out.print("\t" + hola[j]);
                    }
                    hola = null;
                    System.out.println("");
                }
                if (temporal.startsWith("|")) {
                    hola = null;
                    System.out.println("Deploy Info: ");
//                    System.out.println(temporal + " se supone que spliteandolo");
                    hola = temporal.split("\\|");
                    System.out.println("\t" + server + " en " + domain);
//                    System.out.println(hola.length);
                    for (int j = 0; j < hola.length; j++) {
                        switch (j) {
                            case 1:
                                System.out.println("\n\tStagingMode:");
                                System.out.print("\t" + hola[j]);
                                break;
                            case 2:
                                System.out.println("\n\tFullPath:");
                                System.out.print("\t" + hola[j]);
                                break;
                            case 3:
                                System.out.println("\n\tStagedTargets:");
                                System.out.print("\t" + hola[j]);
                                break;
                            case 4:
                                System.out.println("\n\tName:");
                                System.out.print("\t" + hola[j]);
                                break;
                            case 5:
                                System.out.println("\n\tName:");
                                System.out.print("\t" + hola[j]);
                                break;
                        }
                    }
                    System.out.println("");
                    hola = null;
                }
            }
            System.out.println("___________________FINISH___________________________");
        }

        return "";
    }

    public static void main(String[] args) {
        WLShellJDBCParser.parseJDBC();
    }
}
