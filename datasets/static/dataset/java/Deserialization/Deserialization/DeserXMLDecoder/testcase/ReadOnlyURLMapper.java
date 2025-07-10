<filename>nbandroid.core/src/main/java/org/nbandroid/netbeans/gradle/v2/sdk/java/platform/ReadOnlyURLMapper.java<fim_prefix>

        /*
         * Licensed to the Apache Software Foundation (ASF) under one
         * or more contributor license agreements.  See the NOTICE file
         * distributed with this work for additional information
         * regarding copyright ownership.  The ASF licenses this file
         * to you under the Apache License, Version 2.0 (the
         * "License"); you may not use this file except in compliance
         * with the License.  You may obtain a copy of the License at
         *
         *  http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing,
         * software distributed under the License is distributed on an
         * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
         * KIND, either express or implied.  See the License for the
         * specific language governing permissions and limitations
         * under the License.
         */
        package org.nbandroid.netbeans.gradle.v2.sdk.java.platform;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.modules.masterfs.filebasedfs.FileBasedFileSystem;
import org.netbeans.modules.masterfs.filebasedfs.fileobjects.FileObjectFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.modules.Places;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 * Hack to make android platform source folder read-only
 *
 * @author arsi
 */
@ServiceProviders({
        @ServiceProvider(service = URLMapper.class, position = 2000),
        @ServiceProvider(service = ReadOnlyURLMapper.class)})

public class ReadOnlyURLMapper extends URLMapper implements PropertyChangeListener {

    private final AtomicReference<String[]> reference = new AtomicReference<>(new String[0]);
    private static final File LASTPLATFORMS_FILE = Places.getCacheSubfile("nbandroid/lastplatforms.xml");
    private static final ExecutorService POOL = Executors.newFixedThreadPool(1);

    public ReadOnlyURLMapper() {

        if (LASTPLATFORMS_FILE.exists()) {
            <fim_suffix>
        }

    }

    @Override
    public URL getURL(FileObject fo, int type) {
        return null;
    }

    @Override
    public FileObject[] getFileObjects(URL url) {
        String[] paths = reference.get();
        for (String path : paths) {
            if (url.getPath().startsWith(path)) {
                FileObject fileObject = FileBasedFileSystem.getFileObject(new File(url.getPath()), FileObjectFactory.Caller.ToFileObject);
                if (fileObject != null) {
                    return new FileObject[]{ReadOnlyFileObject.findOrCreate(fileObject)};
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        Runnable runnable = new Runnable() {
            public void run() {
                Collection<? extends AndroidJavaPlatformProvider8> providers = Lookup.getDefault().lookupAll(AndroidJavaPlatformProvider8.class);
                List<String> tmp = new ArrayList<>();
                for (AndroidJavaPlatformProvider8 provider : providers) {
                    if (provider != null) {
                        JavaPlatform[] installedPlatforms = provider.getInstalledPlatforms();
                        for (JavaPlatform installedPlatform : installedPlatforms) {
                            List<ClassPath.Entry> entries = installedPlatform.getSourceFolders().entries();
                            for (ClassPath.Entry entrie : entries) {
                                if (!tmp.contains(entrie.getURL().getPath())) {
                                    tmp.add(entrie.getURL().getPath());
                                }
                            }
                        }
                    }
                }
                try {
                    XMLEncoder encoder = new XMLEncoder(new FileOutputStream(LASTPLATFORMS_FILE));
                    encoder.writeObject(tmp.toArray(new String[tmp.size()]));
                    encoder.flush();
                    encoder.close();
                } catch (Exception ex) {
                }
                reference.set(tmp.toArray(new String[tmp.size()]));
            }
        };
        POOL.execute(runnable);
    }

}
<fim_middle>