package com.github.jmoalves.levain.extract;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TarGzExtractor extends Extractor {
    @Override
    protected void extractImpl(Path src, Path dst) throws IOException {
        try (InputStream fis = Files.newInputStream(src);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                Path target = dst.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(tais, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
