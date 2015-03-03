/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * String extractor.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class StringExtractor extends DefaultHandler {

    private static final String APPLE_COMPUTER_DTD_PLIST_1_0_EN = "-//Apple Computer//DTD PLIST 1.0//EN";
    private static final String APPLE_DTD_PLIST_1_0_EN = "-//Apple//DTD PLIST 1.0//EN";

    protected StringBuilder sb = new StringBuilder();
    protected Map<String, String> strings = new TreeMap<>();
    protected String id;
    protected Pattern idPattern = Pattern.compile("[0-9]+\\.title");

    public Map<String, String> getStrings() {
        return strings;
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        sb.append(ch, start, length);
    }

    @Override
    public InputSource resolveEntity(final String publicId, final String systemId)
            throws IOException, SAXException {
        // -//Apple Computer//DTD PLIST 1.0//EN
        // http://www.apple.com/DTDs/PropertyList-1.0.dtd
        if (APPLE_COMPUTER_DTD_PLIST_1_0_EN.equals(publicId) || APPLE_DTD_PLIST_1_0_EN.equals(publicId)) {
            final InputStream propertyListDTD = getClass().getResourceAsStream("PropertyList-1.0.dtd");
            assert propertyListDTD != null;
            return new InputSource(propertyListDTD);
        }
        System.err.println("Failed to resolve " + publicId + " | " + systemId);
        return null;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        sb.setLength(0);
    }
}
