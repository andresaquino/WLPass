/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dun.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.DirectoryWalker;

/**
 *
 * @author abraxas
 */
public class Discoverer extends DirectoryWalker {

    public Discoverer() {
        super();
    }
    public Discoverer(FileFilter filter){
        super(filter,-1);
    }

    public List searchTempFiles(File startDirectory) {
        List results = new ArrayList<File>();
        try {
            walk(startDirectory, results);
        } catch (IOException ex) {
            Logger.getLogger(Discoverer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }

    @Override
    protected void handleFile(File file, int depth, Collection results){
        results.add(file);
    }
}
