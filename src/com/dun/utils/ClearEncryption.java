package com.dun.utils;

import weblogic.security.internal.SerializedSystemIni;
import weblogic.security.internal.encryption.ClearOrEncryptedService;
import weblogic.security.internal.encryption.EncryptionService;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author dubach
 */
public class ClearEncryption {

    private EncryptionService es =
            SerializedSystemIni.getEncryptionService(".");
    private ClearOrEncryptedService ces = new ClearOrEncryptedService(es);

    public String getPassword(String encrypted) {
        return ces.decrypt(encrypted);
    }

    public static String clear(String password) {
        EncryptionService es =
                SerializedSystemIni.getEncryptionService(".");
        ClearOrEncryptedService ces = new ClearOrEncryptedService(es);
        return ces.decrypt(password);
    }

    public static String encryptPassword(String password) {
        EncryptionService es =
                SerializedSystemIni.getEncryptionService(".");
        ClearOrEncryptedService ces = new ClearOrEncryptedService(es);
        return ces.encrypt(password);
    }

    public static void main(String[] args) {
        String password = "", serialized = "", type = "";
        if (args.length >= 2 && args.length % 2 == 0) {
            for (int i = 0; i < args.length; i += 2) {
                if (args[i].startsWith("-") && args[i].contains("s")) {
                    serialized = args[i + 1];

                }
                if (args[i].startsWith("-") && args[i].contains("p")) {
                    password = args[i + 1];

                }
                if (args[i].startsWith("-") && args[i].contains("t")) {
                    type = args[i + 1];
                }
            }
            if (!password.equals("") && !serialized.equals("")) {
                if (type.equals("enc")) {
                    System.out.println(encryptPassword(password));
                }else{
                    System.out.println(clear(password));
                }
            }
        }
    }
}
