package com.github.jmoalves.levain.extract;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TarGzExtractor extends Extractor {
    @Override
    protected void extractImpl(Path src, Path dst) throws IOException {
        try (InputStream fis = Files.newInputStream(src);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            byte[] buffer = new byte[8192];
            while (true) {
                var entry = tais.getNextEntry();
                if (entry == null) {
                    break;
                }
                TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
                Path target = dst.resolve(tarEntry.getName()).normalize();
                if (tarEntry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                        try (var out = Files.newOutputStream(target, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING)) {
                        int read;
                        while ((read = tais.read(buffer)) > 0) {
                            out.write(buffer, 0, read);
                            reportBytes(read);
                        }
                    }
                }
            }
        }
    }
}
