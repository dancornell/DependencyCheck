/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codesecure.dependencycheck.data.lucene;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import junit.framework.TestCase;
import org.codesecure.dependencycheck.utils.Settings;

/**
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public abstract class BaseIndexTestCase extends TestCase {
    
    public BaseIndexTestCase(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ensureIndexExists();        
    }
    
    protected void ensureIndexExists() throws Exception {
        String indexPath = Settings.getString("cpe");
        java.io.File f = new File(indexPath);
        if (!f.exists()) {
            f.mkdirs();
            FileInputStream fis = null;
            ZipInputStream zin = null;
            try {
                File path = new File(this.getClass().getClassLoader().getResource("index.cpe.zip").getPath());
                fis = new FileInputStream(path);
                zin = new ZipInputStream(new BufferedInputStream(fis));
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    int BUFFER = 2048;
                    String outputName = indexPath + File.separatorChar + entry.getName();
                    FileOutputStream fos = null;
                    BufferedOutputStream dest = null;
                    try {
                        File o = new File(outputName);
//                        File oPath = new File(o.getParent());
//                        if (!oPath.exists()) {
//                            oPath.mkdir();
//                        }
                        o.createNewFile();
                        fos = new FileOutputStream(o,false);
                        dest = new BufferedOutputStream(fos, BUFFER);
                        byte data[] = new byte[BUFFER];
                        int count;
                        while ((count = zin.read(data, 0, BUFFER)) != -1) {
                           dest.write(data, 0, count);
                        }
                    } catch (Exception ex) { 
                        String ignore = ex.getMessage(); 
                    } finally {
                        try {
                            dest.flush();
                            dest.close();
                            dest = null;
                        } catch (Throwable ex) { String ignore = ex.getMessage(); }
                        try {
                            fos.close();
                            fos = null;
                        } catch (Throwable ex) { String ignore = ex.getMessage(); }
                    }
                }
            } finally {
                try {
                    if (zin!=null) {
                        zin.close();
                    }
                    zin = null;
                } catch (Throwable ex) { String ignore = ex.getMessage(); }
                try {
                    if (fis!=null) {
                        fis.close();
                    }
                    fis = null;
                } catch (Throwable ex) { String ignore = ex.getMessage(); }
            }
        }
    }
}
