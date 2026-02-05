package com.github.jmoalves.levain.extract;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SevenZipExtractor extends Extractor {
    @Override
    protected void extractImpl(Path src, Path dst) throws IOException {
        try (SevenZFile sevenZFile = SevenZFile.builder().setPath(src).get()) {
            SevenZArchiveEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = sevenZFile.getNextEntry()) != null) {
                Path target = dst.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (var out = Files.newOutputStream(target, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    int read;
                    while ((read = sevenZFile.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }
}
