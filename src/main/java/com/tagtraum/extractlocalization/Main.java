/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;

/**
 * Main.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Main {

    // Feel free to document/refactor this...

    private final Map<String, Map<String, String>> localizations = new TreeMap<>();
    private final SAXParserFactory saxParserFactory;

    public Main() {
        // setup parser
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(false);
        saxParserFactory.setValidating(true);
    }

    public void extractFromAppBundles(final String... appBundles) {
        for (final String s: appBundles) {
            try {
                extract(Paths.get(s));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        writeResults();
    }

    public void extractFromApplications() throws IOException {
        Files.walkFileTree(Paths.get("/Applications"), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (dir.toString().endsWith(".app")) {
                    try {
                        extract(dir);
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return super.preVisitDirectory(dir, attrs);
            }
        });
        writeResults();
    }

    private void writeResults() {
        localizations.keySet().forEach(locale -> {
            try {
                final Path path = Paths.get("localizations/" + locale + ".json");
                Files.createDirectories(path.getParent());
                try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(Json.encode(localizations.get(locale)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void extract(final Path appBundle) throws IOException {
        final Path resourcesPath = appBundle.resolve("Contents/Resources/");
        final Path basePath = appBundle.resolve("Contents/Resources/Base.lproj");
        if (!Files.exists(basePath)) {
            // fail silently, if we don't find a base project
            return;
        }
        System.out.println("Extracting strings for " + appBundle + "...");
        Files.walk(basePath)
                .filter(path -> path.toString().endsWith(".nib"))
                .forEach(nibFile -> {
                    try {
                        final String file = nibFile.getFileName().toString();
                        extract(nibFile, resourcesPath, file.substring(0, file.length() - ".nib".length()));
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                });
    }

    private void extract(final Path nibFile, final Path resourcesPath, final String resourceName) throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        // convert nib to plain xml
        final Path xmlNibFile = toXML(nibFile);
        final Map<String, String> idToBaseName = extractBaseMap(xmlNibFile);

        // find localizations...
        Files.walk(resourcesPath, 1).filter(path -> {
            final String s = path.toString();
            return s.endsWith(".lproj") && !s.endsWith("Base.lproj");
        }).forEach(lproj -> {
            try {
                final String pathName = lproj.getFileName().toString();
                final String locale = pathName.substring(0, pathName.length() - ".lproj".length());
                final Path stringsFile = lproj.resolve(resourceName + ".strings");
                if (Files.exists(stringsFile)) {
                    final Map<String, String> idToLocalName = extractLocalizedMap(stringsFile);
                    final Map<String, String> baseNamesToLocalNames = mapBaseNamesToLocalNames(idToBaseName, idToLocalName);
                    getLocalMap(locale).putAll(baseNamesToLocalNames);
                } else {
                    // fail silently, if we don't find any matching strings file
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Map<String, String> mapBaseNamesToLocalNames(final Map<String, String> idToBaseName, final Map<String, String> idToLocalName) {
        final Map<String, String> baseNamesToLocalNames = new TreeMap<>();
        // map localized names to base names
        for (final Map.Entry<String, String> e : idToLocalName.entrySet()) {
            final String id = e.getKey();
            final String baseName = idToBaseName.get(id);
            if (baseName != null) {
                final String localName = e.getValue();
                baseNamesToLocalNames.put(baseName, localName);
            }
        }
        return baseNamesToLocalNames;
    }

    private Map<String, String> getLocalMap(final String locale) {
        Map<String, String> localMap = localizations.get(locale);
        if (localMap == null) {
            localMap = new TreeMap<>();
            localizations.put(locale, localMap);
        }
        return localMap;
    }

    private Map<String, String> extractLocalizedMap(final Path stringsFile) throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        final StringExtractor stringExtractor = new StringsStringExtractor();
        final Path xmlStringsFile = toXML(stringsFile);
        try {
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(xmlStringsFile.toFile(), stringExtractor);
        } finally {
            Files.delete(xmlStringsFile);
        }
        return stringExtractor.getStrings();
    }

    private Map<String, String> extractBaseMap(final Path xmlNibFile) throws ParserConfigurationException, SAXException, IOException {
        final NibStringExtractor nibStringExtractor = new NibStringExtractor();
        try {
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            saxParser.parse(xmlNibFile.toFile(), nibStringExtractor);
        } finally {
            Files.delete(xmlNibFile);
        }
        return nibStringExtractor.getStrings();
    }

    private Path toXML(final Path binaryPList) throws IOException, InterruptedException {
        final Path xmlPList = Files.createTempFile("xml", ".plist");
        final Process process = Runtime.getRuntime().exec(new String[]{"plutil", "-convert",
                "xml1", "-s", "-o", xmlPList.toString(), binaryPList.toString()});
        process.waitFor();
        return xmlPList;
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        final Main main = new Main();
        if (args.length == 0) {
            main.extractFromApplications();
        } else {
            main.extractFromAppBundles(args);
        }
        System.out.println("Done.");
    }

}
