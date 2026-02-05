package com.github.jmoalves.levain.extract;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

@ApplicationScoped
public class ExtractorFactory {
    public boolean isTypeSupported(String type) {
        return typeFromString(type) != null;
    }

    public Extractor createExtractor(Path src, String type) {
        ArchiveType resolved = typeFromString(type);
        if (resolved == null) {
            resolved = typeFromFile(src);
        }

        if (resolved == null) {
            throw new IllegalArgumentException(src + " - file not supported");
        }

        return switch (resolved) {
            case ZIP -> new ZipExtractor();
            case SEVEN_Z -> new SevenZipExtractor();
            case TAR_GZ -> new TarGzExtractor();
        };
    }

    private ArchiveType typeFromFile(Path src) {
        if (src == null) {
            return null;
        }
        String name = src.getFileName().toString().toLowerCase();
        if (name.endsWith(".zip")) {
            return ArchiveType.ZIP;
        }
        if (name.endsWith(".7z") || name.endsWith(".7z.exe")) {
            return ArchiveType.SEVEN_Z;
        }
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            return ArchiveType.TAR_GZ;
        }
        return null;
    }

    private ArchiveType typeFromString(String type) {
        if (type == null) {
            return null;
        }
        String normalized = type.toLowerCase();
        return switch (normalized) {
            case "zip" -> ArchiveType.ZIP;
            case "7z", "7z.exe" -> ArchiveType.SEVEN_Z;
            case "tar.gz", "tgz" -> ArchiveType.TAR_GZ;
            default -> null;
        };
    }
}
