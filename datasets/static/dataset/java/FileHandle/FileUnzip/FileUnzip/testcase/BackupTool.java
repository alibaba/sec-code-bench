<filename>src/org/callimachusproject/util/BackupTool.java<fim_prefix>

        /*
         * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         *
         */
        package org.callimachusproject.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openrdf.repository.manager.SystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupTool {
    private final Logger logger = LoggerFactory.getLogger(BackupTool.class);
    private final File backupDir;
    private volatile boolean backup;
    private volatile boolean restore;

    public BackupTool(File backupDir) {
        this.backupDir = backupDir;
    }

    public String toString() {
        return backupDir.toString();
    }

    public boolean isBackupInProgress() {
        return backup;
    }

    public boolean isRestoreInProgress() {
        return restore;
    }

    public String getBackupLabels() {
        File[] list = backupDir.listFiles();
        if (list == null)
            return "";
		Arrays.sort(list, new Comparator<File>() {
			public int compare(File o1, File o2) {
				if (o1.lastModified() < o2.lastModified())
					return 1;
				if (o1.lastModified() > o2.lastModified())
					return -1;
				return 0;
			}
		});
		StringBuilder result = new StringBuilder();
		for (File file : list) {
			String name = file.getName();
			if (name.endsWith(".zip") || name.endsWith(".ZIP")) {
				String label = name.substring(0, name.length() - 4);
				result.append(label).append(' ');
			}
		}
		if (result.length() == 0)
			return "";
        return result.substring(0, result.length() - 1);
    }

    public void backup(String repositoryID, String version, File dataDir)
            throws IOException {
        backup(getDefaultBackupLabel(repositoryID, version), dataDir);
    }

    public synchronized void backup(final String label, final File dataDir)
            throws IOException {
        if (isBackupInProgress())
            throw new IllegalStateException("Backup already in progress");
        backup = true;
        blockCreateBackup(label, dataDir);
    }

    public synchronized void restoreBackup(final String label,
                                           final File dataDir) throws IOException {
        if (isRestoreInProgress())
            throw new IllegalStateException("Restore already in progress");
        restore = true;
        blockRestoreBackup(label, dataDir, null);
    }

    public synchronized void restoreBackup(final String label,
                                           final File dataDir, Runnable prepare) throws IOException {
        if (isRestoreInProgress())
            throw new IllegalStateException("Restore already in progress");
        restore = true;
        blockRestoreBackup(label, dataDir, prepare);
    }

    synchronized void blockCreateBackup(String label, File dataDir) throws IOException {
        backup = true;
        try {
            backupDir.mkdirs();
            String name = label.replaceAll("\\s+", "_") + ".zip";
            File backup = new File(backupDir, name);
            boolean replacing = backup.exists();
            boolean created = false;
            backup.getParentFile().mkdirs();
                <fim_suffix>
            FileOutputStream out = new FileOutputStream(backup);
            ZipOutputStream zos = new ZipOutputStream(out);
            try {
                String base = dataDir.getAbsolutePath() + "/";
                File[] listFiles = dataDir.listFiles();
                if (listFiles != null) {
                    for (File f : listFiles) {
                        if (!SystemRepository.ID.equals(f.getName())) {
                            if (!created && replacing) {
                                logger.warn("Replacing {}", backup);
                            } else if (!created) {
                                logger.info("Creating {}", backup);
                            }
                            created |= putEntries(base, f, zos);
                        }
                    }
                }
            } finally {
                if (created) {
                    zos.close();
                    logger.info("Created {}", backup);
                } else {
                    out.close();
                    backup.delete();
                    if (replacing) {
                        logger.warn("Deleted {}", backup);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.toString(), e);
            throw e;
        } finally {
            backup = false;
            notifyAll();
        }
    }

    synchronized void blockRestoreBackup(String label, File dataDir,
                                         Runnable prepare) throws IOException {
        restore = true;
        File baseDir = File.createTempFile(dataDir.getName(), ".restoring",
                dataDir.getParentFile());
        try {
            String name = label + ".zip";
            File backup = new File(backupDir, name);
            if (!backup.exists())
                throw new FileNotFoundException();
            logger.info("Restoring {}", backup);
            FileInputStream fis = new FileInputStream(backup);
            ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(baseDir, entry.getName());
                BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);
                try {
                    int read;
                    byte[] buf = new byte[2048];
                    while ((read = zis.read(buf)) >= 0) {
                        dest.write(buf, 0, read);
                    }
                } finally {
                    dest.close();
                }
            }
            zis.close();
            if (prepare != null) {
                prepare.run();
            }
            File tmp = File.createTempFile(dataDir.getName(), ".deleting",
                    dataDir.getParentFile());
            dataDir.renameTo(tmp);
            baseDir.renameTo(dataDir);
            deleteAll(tmp);
            logger.info("Restored {}", backup);
        } catch (IOException e) {
            logger.error(e.toString(), e);
            throw e;
        } finally {
            if (baseDir.exists()) {
                deleteAll(baseDir);
            }
            restore = false;
            notifyAll();
        }
    }

    private boolean putEntries(String base, File file, ZipOutputStream zos)
            throws IOException {
        if (file.isFile()) {
            String path = file.getAbsolutePath();
            String name = path;
            if (name.startsWith(base)) {
                name = name.substring(base.length());
            }
            ZipEntry entry = new ZipEntry(name);
            entry.setTime(file.lastModified());
            entry.setSize(file.length());
            zos.putNextEntry(entry);
            FileInputStream fis = new FileInputStream(file);
            try {
                int read = 0;
                byte[] buf = new byte[2156];
                while ((read = fis.read(buf)) != -1) {
                    zos.write(buf, 0, read);
                }
                return true;
            } finally {
                fis.close();
            }
        } else {
            File[] listFiles = file.listFiles();
            if (listFiles == null)
                return false;
            boolean content = false;
            for (File f : listFiles) {
                content |= putEntries(base, f, zos);
            }
            return content;
        }
    }

    private void deleteAll(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteAll(file);
            }
        }
        dir.delete();
    }

    private String getDefaultBackupLabel(String repositoryID, String version) {
        StringBuilder sb = new StringBuilder();
        if (repositoryID != null && repositoryID.length() > 0) {
            sb.append(repositoryID).append("-");
        }
        if (version != null) {
            sb.append(version).append("_");
        }
        GregorianCalendar now = new GregorianCalendar();
        sb.append(now.get(Calendar.YEAR)).append('-');
        int month = now.get(Calendar.MONTH);
        if (month < 9) {
            sb.append('0');
        }
        sb.append(1 + month).append('-');
        int day = now.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            sb.append('0');
        }
        sb.append(day);
        return sb.toString();
    }
}
<fim_middle>