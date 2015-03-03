/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import org.xml.sax.SAXException;

/**
 * SAX handler that extracts strings and their ids from a {@code NIB} file.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class NibStringExtractor extends StringExtractor {

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        final String content = sb.toString();
        boolean idExisted = id != null;
        if ("string".equals(qName)) {
            if (id != null && !content.isEmpty()) {
                strings.put(id, content);
            } else if (idPattern.matcher(content).matches()) {
                id = content;
            }
        }
        // make sure we delete ids, if don't find a value right away.
        if (idExisted) {
            id = null;
        }
    }
}
