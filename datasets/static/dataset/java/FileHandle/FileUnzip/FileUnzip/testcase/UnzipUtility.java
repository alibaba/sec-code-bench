<filename>UnzipUtility.java<fim_prefix>

// UnzipUtility.java
package com.jdojo.archives;

import java.util.zip.ZipEntry;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.util.zip.ZipInputStream;

public class UnzipUtility {
    public static void main(String[] args) {
        String zipFileName = "ziptest.zip";
        String unzipdirectory = "extracted";
        unzip(zipFileName, unzipdirectory);
    }

    public static void unzip(String zipFileName, String unzipdir) {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(
                        new FileInputStream(zipFileName)))) {

            // Read each entry from the ZIP file 
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Extract the entry's contents
                extractEntryContent(zis, entry, unzipdir);
            }

            System.out.println("ZIP file's contents have been extracted to "
                    + (new File(unzipdir)).getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractEntryContent(ZipInputStream zis,
            ZipEntry entry,
            String unzipdir)
            throws IOException, FileNotFoundException {

        String entryFileName = entry.getName();
        String entryPath = unzipdir + File.separator + entryFileName;
        <fim_suffix>

        // Create the entry file by creating necessary directories
        createFile(entryPath);

        // Create an output stream to extract the content of the 
        // zip entry and write to the new file
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(entryPath))) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, count);
            }
        }
    }

    public static void createFile(String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();

        // Create all parent directories if they do not exist
        if (!parent.exists()) {
            parent.mkdirs();
        }

        file.createNewFile();
    }
}

<fim_middle>