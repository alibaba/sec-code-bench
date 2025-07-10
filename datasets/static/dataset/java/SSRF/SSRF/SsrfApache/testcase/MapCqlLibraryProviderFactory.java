<filename>quality-measure-and-cohort-service-main/cohort-evaluator/src/main/java/com/ibm/cohort/cql/library/MapCqlLibraryProviderFactory.java<fim_prefix>

        /*
         * (C) Copyright IBM Corp. 2022, 2022
         *
         * SPDX-License-Identifier: Apache-2.0
         */

        package com.ibm.cohort.cql.library;

import com.ibm.cohort.cql.helpers.PathHelper;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

/**
 * A utility for assisting in creating {@link MapCqlLibraryProvider} instances
 * from arbitrary packaged form factors (e.g. directories, zip files, etc).
 */
public class MapCqlLibraryProviderFactory {

    private final ZipStreamProcessor zipProcessor;

    public MapCqlLibraryProviderFactory() {
        this(new ZipStreamProcessor());
    }

    public MapCqlLibraryProviderFactory(ZipStreamProcessor zipProcessor) {
        this.zipProcessor = zipProcessor;
    }

    public CqlLibraryProvider fromDirectory(Path directory, String... searchPaths) throws IOException {
        Map<CqlLibraryDescriptor, CqlLibrary> map = new HashMap<>();

        try (Stream<Path> stream = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
            map = stream
                    .filter(x -> PathHelper.isInSearchPaths(directory, x, searchPaths))
                    .filter(x -> CqlLibraryHelpers.isFileReadable(x.toString()))
                    .map(this::toCqlLibrary)
                    .collect(Collectors.toMap(CqlLibrary::getDescriptor, Function.identity()));
        }

        return new MapCqlLibraryProvider(map);
    }

    public CqlLibraryProvider fromZipFile(Path zipFile, String... searchPaths) throws IOException {
        try(InputStream is = new FileInputStream(zipFile.toFile())) {
            ZipInputStream zis = new ZipInputStream(is);
            return fromZipStream(zis, searchPaths);
        }
    }

    public CqlLibraryProvider fromZipStream(ZipInputStream zipStream, String... searchPaths) throws IOException {
        Map<CqlLibraryDescriptor, CqlLibrary> map = new HashMap<>();

        zipProcessor.processZip(zipStream, searchPaths, (filename, content) -> {
            if (CqlLibraryHelpers.isFileReadable(filename)) {
                CqlLibraryDescriptor descriptor = CqlLibraryHelpers.filenameToLibraryDescriptor(filename);
                CqlLibrary cqlLibrary = new CqlLibrary()
                        .setDescriptor(descriptor)
                        .setContent(content);
                map.put(descriptor, cqlLibrary);
            }
        });

        return new MapCqlLibraryProvider(map);
    }

    private CqlLibrary toCqlLibrary(Path path) {
        CqlLibraryDescriptor descriptor = CqlLibraryHelpers.filenameToLibraryDescriptor(path.toString());
        String content = readFile(path);
        return new CqlLibrary()
                .setContent(content)
                .setDescriptor(descriptor);
    }

    private String readFile(Path path) {
        <fim_suffix>
        try {
            return IOUtils.toString(path.toUri(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
<fim_middle>