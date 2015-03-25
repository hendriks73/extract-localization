/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Main.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Main {

    // Feel free to document/refactor this...

    private final Map<String, Map<String, String>> localizations = new TreeMap<>();
    private final SAXParserFactory saxParserFactory;
    private boolean filter;
    private static final Map<String, String> LANGUAGE_ALIAS = new HashMap<>();
    static {
        LANGUAGE_ALIAS.put("Dutch", "nl");
        LANGUAGE_ALIAS.put("French", "fr");
        LANGUAGE_ALIAS.put("German", "de");
        LANGUAGE_ALIAS.put("Italian", "it");
        LANGUAGE_ALIAS.put("Japanese", "ja");
        LANGUAGE_ALIAS.put("Spanish", "es");
    }
    private static final Map<String, String> FILTER_TEMPLATE = new TreeMap<>();
    static {
        FILTER_TEMPLATE.put("About <AppName>", null);
        FILTER_TEMPLATE.put("Bring <AppName> Window to Front", null);
        FILTER_TEMPLATE.put("Bring All to Front", null);
        FILTER_TEMPLATE.put("Capitalize", null);
        FILTER_TEMPLATE.put("Check Document Now", null);
        FILTER_TEMPLATE.put("Check Grammar With Spelling", null);
        FILTER_TEMPLATE.put("Check Spelling While Typing", null);
        FILTER_TEMPLATE.put("Close", null);
        FILTER_TEMPLATE.put("Copy", null);
        FILTER_TEMPLATE.put("Correct Spelling Automatically", null);
        FILTER_TEMPLATE.put("Cut", null);
        FILTER_TEMPLATE.put("Delete", null);
        FILTER_TEMPLATE.put("Edit", null);
        FILTER_TEMPLATE.put("File", null);
        FILTER_TEMPLATE.put("Find and Replace…", null);
        FILTER_TEMPLATE.put("Find Next", null);
        FILTER_TEMPLATE.put("Find Previous", null);
        FILTER_TEMPLATE.put("Find", null);
        FILTER_TEMPLATE.put("Find…", null);
        FILTER_TEMPLATE.put("Help", null);
        FILTER_TEMPLATE.put("Hide <AppName>", null);
        FILTER_TEMPLATE.put("Hide Others", null);
        FILTER_TEMPLATE.put("Jump to Selection", null);
        FILTER_TEMPLATE.put("Make Lower Case", null);
        FILTER_TEMPLATE.put("Make Upper Case", null);
        FILTER_TEMPLATE.put("Minimize", null);
        FILTER_TEMPLATE.put("Open…", null);
        FILTER_TEMPLATE.put("Paste and Match Style", null);
        FILTER_TEMPLATE.put("Paste", null);
        FILTER_TEMPLATE.put("Preferences…", null);
        FILTER_TEMPLATE.put("Quit <AppName>", null);
        FILTER_TEMPLATE.put("Redo", null);
        FILTER_TEMPLATE.put("Select All", null);
        FILTER_TEMPLATE.put("Services", null);
        FILTER_TEMPLATE.put("Show All", null);
        FILTER_TEMPLATE.put("Show Spelling and Grammar", null);
        FILTER_TEMPLATE.put("Show Substitutions", null);
        FILTER_TEMPLATE.put("Smart Copy/Paste", null);
        FILTER_TEMPLATE.put("Smart Dashes", null);
        FILTER_TEMPLATE.put("Smart Links", null);
        FILTER_TEMPLATE.put("Smart Quotes", null);
        FILTER_TEMPLATE.put("Speech", null);
        FILTER_TEMPLATE.put("Spelling and Grammar", null);
        FILTER_TEMPLATE.put("Spelling", null);
        FILTER_TEMPLATE.put("Start Speaking", null);
        FILTER_TEMPLATE.put("Stop Speaking", null);
        FILTER_TEMPLATE.put("Substitutions", null);
        FILTER_TEMPLATE.put("Text Replacement", null);
        FILTER_TEMPLATE.put("Transformations", null);
        FILTER_TEMPLATE.put("Undo", null);
        FILTER_TEMPLATE.put("Use Selection for Find", null);
        FILTER_TEMPLATE.put("Use Selection for Replace", null);
        FILTER_TEMPLATE.put("Window", null);
        FILTER_TEMPLATE.put("Zoom",  null);
    }

    public Main() {
        // setup parser
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(false);
        saxParserFactory.setValidating(false);
    }

    public boolean isFilter() {
        return filter;
    }

    public void setFilter(final boolean filter) {
        this.filter = filter;
    }

    public void extractFromAppBundles(final List<String> appBundles) {
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
                final Path path = Paths.get("localizations/" + locale.replace('_', '-') + ".json");
                Files.createDirectories(path.getParent());
                try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
                    final Map<String, String> localMap = filter
                            ? filter(localizations.get(locale))
                            : localizations.get(locale);
                    writer.write(Json.encode(localMap));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Map<String, String> filter(final Map<String, String> map) {
        final TreeMap<String, String> filtered = new TreeMap<>(FILTER_TEMPLATE);
        filtered.keySet().stream().filter(map::containsKey).forEach(key -> filtered.put(key, map.get(key)));
        return filtered;
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
        final Map<String, String> idToBaseName = extractBaseMap(nibFile);

        // find app name
        final String appName = resourcesPath.getParent().getParent().getFileName().toString().replace(".app", "");

        // find localizations...
        Files.walk(resourcesPath, 1).filter(path -> {
            final String s = path.toString();
            return s.endsWith(".lproj") && !s.endsWith("Base.lproj");
        }).forEach(lproj -> {
            try {
                final String pathName = lproj.getFileName().toString();
                final String locale = resolveLocale(pathName.substring(0, pathName.length() - ".lproj".length()));
                final Path stringsFile = lproj.resolve(resourceName + ".strings");
                if (Files.exists(stringsFile)) {
                    final Map<String, String> idToLocalName = extractLocalizedMap(stringsFile);
                    final Map<String, String> baseNamesToLocalNames = mapBaseNamesToLocalNames(idToBaseName, idToLocalName);
                    final String localAppName = baseNamesToLocalNames.get(appName);
                    if (localAppName != null) {
                        final Map<String, String> appNamePlaceHolders = new TreeMap<>();
                        baseNamesToLocalNames.keySet().stream().filter(s -> s.contains(appName) && !s.equals(appName)).forEach(s -> appNamePlaceHolders.put(s.replace(appName, "<AppName>"), baseNamesToLocalNames.get(s).replace(localAppName, "<AppName>")));
                        baseNamesToLocalNames.putAll(appNamePlaceHolders);
                    }
                    getLocalMap(locale).putAll(baseNamesToLocalNames);
                } else {
                    // fail silently, if we don't find any matching strings file
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String resolveLocale(final String locale) {
        final String s = LANGUAGE_ALIAS.get(locale);
        return s == null ? locale : s;
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
        try (final Reader r = Files.newBufferedReader(xmlStringsFile,StandardCharsets.UTF_8)) {
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final InputSource source = new InputSource(new ValidXMLReader(r));
            source.setSystemId(stringsFile.toString());
            saxParser.parse(source, stringExtractor);
        } catch (SAXParseException e) {
            System.err.println("Error parsing " + stringsFile + ": " + e.toString());
        } finally {
            Files.delete(xmlStringsFile);
        }
        return stringExtractor.getStrings();
    }

    private Map<String, String> extractBaseMap(final Path nibFile) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
        final NibStringExtractor nibStringExtractor = new NibStringExtractor();
        final Path xmlNibFile = toXML(nibFile);
        try (final Reader r = Files.newBufferedReader(xmlNibFile,StandardCharsets.UTF_8)) {
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final InputSource source = new InputSource(new ValidXMLReader(r));
            source.setSystemId(nibFile.toString());
            saxParser.parse(source, nibStringExtractor);
        } catch (SAXParseException e) {
            System.err.println("Error parsing " + nibFile + ": " + e.toString());
        } finally {
            Files.delete(xmlNibFile);
        }
        return nibStringExtractor.getStrings();
    }

    private Path toXML(final Path binaryPList) throws IOException, InterruptedException {

        // Perhaps we should switch to JSON as format to avoid XML parsing problems...

        final Path xmlPList = Files.createTempFile("xml", ".plist");
        final Process process = Runtime.getRuntime().exec(new String[]{"plutil", "-convert",
                "xml1", "-s", "-o", xmlPList.toString(), binaryPList.toString()});
        process.waitFor();
        return xmlPList;
    }

    public static void main(String[] args) throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        final Main main = new Main();
        final List<String> argList = new ArrayList<>();
        for (final String arg : args) {
            if ("-f".equals(arg)) main.setFilter(true);
            else argList.add(arg);
        }
        if (argList.isEmpty()) {
            main.extractFromApplications();
        } else {
            main.extractFromAppBundles(argList);
        }
        System.out.println("Done.");
    }

}
