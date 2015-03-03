/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import org.xml.sax.SAXException;

/**
 * SAX handler that extracts strings and their ids from a {@code .strings} file.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class StringsStringExtractor extends StringExtractor {

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        final String content = sb.toString();
        if ("key".equals(qName) && idPattern.matcher(content).matches()) {
            id = content;
        }
        if ("string".equals(qName) && id != null) {
            strings.put(id, content);
            id = null;
        }
    }

}
