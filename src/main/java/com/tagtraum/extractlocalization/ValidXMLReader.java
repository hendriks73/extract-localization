/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * ValidXMLReader drops invalid unicode chars like {@code 0x1b}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ValidXMLReader extends FilterReader {

    public ValidXMLReader(final Reader in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        while (!isValid((char)c)) {
            c = super.read();
        }
        return c;
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        int read = super.read(cbuf, off, len);
        for (int i=0; i<read;) {
            if (!isValid(cbuf[off + i])) {
                System.arraycopy(cbuf, off + i + 1, cbuf, off + i, read - off - i - 1);
                read--;
            } else {
                i++;
            }
        }
        return read;
    }

    private static boolean isValid(final char c) {
        if (c>=0x20) return true; // this is a simplification, real def is at http://www.w3.org/TR/xml/#charsets
        if (c==0x9) return true;
        if (c==0xA) return true;
        if (c==0xD) return true;
        return false;
    }
}
