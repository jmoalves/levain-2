package com.github.jmoalves.levain.extract;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractorFactoryTest {

    @Test
    void shouldReportSupportedTypes() {
        ExtractorFactory factory = new ExtractorFactory();

        assertTrue(factory.isTypeSupported("zip"));
        assertTrue(factory.isTypeSupported("7z"));
        assertTrue(factory.isTypeSupported("7z.exe"));
        assertTrue(factory.isTypeSupported("tar.gz"));
        assertTrue(factory.isTypeSupported("tgz"));
        assertFalse(factory.isTypeSupported("rar"));
    }

    @Test
    void shouldCreateExtractorFromType() {
        ExtractorFactory factory = new ExtractorFactory();

        assertInstanceOf(ZipExtractor.class, factory.createExtractor(Path.of("file.zip"), "zip"));
        assertInstanceOf(SevenZipExtractor.class, factory.createExtractor(Path.of("file.7z"), "7z"));
        assertInstanceOf(TarGzExtractor.class, factory.createExtractor(Path.of("file.tgz"), "tgz"));
    }

    @Test
    void shouldCreateExtractorFromFilename() {
        ExtractorFactory factory = new ExtractorFactory();

        assertInstanceOf(ZipExtractor.class, factory.createExtractor(Path.of("archive.zip"), null));
        assertInstanceOf(SevenZipExtractor.class, factory.createExtractor(Path.of("archive.7z.exe"), null));
        assertInstanceOf(TarGzExtractor.class, factory.createExtractor(Path.of("archive.tar.gz"), null));
    }

    @Test
    void shouldThrowWhenUnsupported() {
        ExtractorFactory factory = new ExtractorFactory();

        assertThrows(IllegalArgumentException.class,
                () -> factory.createExtractor(Path.of("archive.rar"), null));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createExtractor(null, "rar"));
    }
}
