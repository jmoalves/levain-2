package com.github.jmoalves.levain.extract;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor extends Extractor {
    @Override
    protected void extractImpl(Path src, Path dst) throws IOException {
        try (InputStream fis = Files.newInputStream(src);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = dst.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (var out = Files.newOutputStream(target)) {
                        int read;
                        while ((read = zis.read(buffer)) > 0) {
                            out.write(buffer, 0, read);
                            reportBytes(read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
