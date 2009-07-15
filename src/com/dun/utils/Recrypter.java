/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dun.utils;


import java.io.File;
import com.dun.file.FileUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author dubach
 */
public class Recrypter {

    public static void main(String[] args) {
        String serialized = "";
        String arch = "";
        String workingDir = System.getProperty("user.dir");

        if (args.length == 4) {
            for (int i = 0; i < args.length; i += 2) {
                if (args[i].equals("-s")) {
                    serialized = args[i + 1];
                }
                if (args[i].equals("-f")) {
                    arch = args[i + 1];
                }
            }
        } else {
            System.out.println("USAGE:");
        }
        if (!serialized.equals("") && !arch.equals("")) {
            System.out.println("entrando");
            File file = new File(serialized);
            String serializedDir = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separatorChar));

            if (!workingDir.equals(serializedDir)) {
                try {
                    System.out.println("copiando");
                    FileUtil.copy(serialized, workingDir + File.separator + "SerializedSystemIni.dat");
                } catch (IOException ex) {
                    Logger.getLogger(Recrypter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            ArrayList<String> listaPass = new ArrayList<String>(2);
            Scanner sc = null;
            try {
                sc = new Scanner(new File(arch));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Recrypter.class.getName()).log(Level.SEVERE, null, ex);
            }
            while(sc.hasNextLine()){
                String temp = sc.nextLine();
                if(temp.contains(":") && !temp.contains("URL")){
			String[] token = temp.split(":");
                    listaPass.add(token[0]+":"+ClearEncryption.encryptPassword(token[1]));
		}

            }
		for(String s : listaPass){
			System.out.println(s);
		}
        }
    }

}
