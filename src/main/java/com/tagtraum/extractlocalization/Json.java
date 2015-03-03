/*
 * =================================================
 * Copyright 2015 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.tagtraum.extractlocalization;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Json support.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Json {

    private Json() {}

    public static String encode(final Map<String, String> map) {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        map.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(final String key, final String value) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append("\n  ").append(encode(key)).append(" : ").append(encode(value));
            }
        });
        sb.append("\n}\n");
        return sb.toString();
    }

    public static String encode(final String s) {
        if (s == null) return "null";
        return "\"" + escape(s) + "\"";
    }

    private static String escape(final String s) {
        if(s == null) {
            return null;
        } else {
            final StringBuilder sb = new StringBuilder();
            escape(s, sb);
            return sb.toString();
        }
    }

    private static void escape(final String s, final StringBuilder sb) {
        for(int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch(ch) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if((ch < 0 || ch > 31) && (ch < 127 || ch > 159) && (ch < 8192 || ch > 8447)) {
                        sb.append(ch);
                    } else {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");

                        for(int k = 0; k < 4 - ss.length(); ++k) {
                            sb.append('0');
                        }

                        sb.append(ss.toUpperCase());
                    }
            }
        }

    }
}
