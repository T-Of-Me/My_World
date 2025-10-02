/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.util.Objects;

public final class Syntax {
    public static void requireValidRFC2616Token(String value, String msg) {
        Objects.requireNonNull(msg, "msg cannot be null");
        if (value == null) {
            return;
        }
        int valueLen = value.length();
        if (valueLen == 0) {
            return;
        }
        for (int i = 0; i < valueLen; ++i) {
            char c = value.charAt(i);
            if (c <= '\u001f' || c == '\u007f') {
                throw new IllegalArgumentException(msg + ": RFC2616 tokens may not contain control characters");
            }
            if (c == '(' || c == ')' || c == '<' || c == '>' || c == '@' || c == ',' || c == ';' || c == ':' || c == '\\' || c == '\"' || c == '/' || c == '[' || c == ']' || c == '?' || c == '=' || c == '{' || c == '}' || c == ' ') {
                throw new IllegalArgumentException(msg + ": RFC2616 tokens may not contain separator character: [" + c + "]");
            }
            if (c < '\u0080') continue;
            throw new IllegalArgumentException(msg + ": RFC2616 tokens characters restricted to US-ASCII: 0x" + Integer.toHexString(c));
        }
    }

    public static void requireValidRFC6265CookieValue(String value) {
        if (value == null) {
            return;
        }
        int valueLen = value.length();
        if (valueLen == 0) {
            return;
        }
        int i = 0;
        if (value.charAt(0) == '\"') {
            if (valueLen <= 1 || value.charAt(valueLen - 1) != '\"') {
                throw new IllegalArgumentException("RFC6265 Cookie values must have balanced DQUOTES (if used)");
            }
            ++i;
            --valueLen;
        }
        while (i < valueLen) {
            char c = value.charAt(i);
            if (c <= '\u001f' || c == '\u007f') {
                throw new IllegalArgumentException("RFC6265 Cookie values may not contain control characters");
            }
            if (c == ' ' || c == '\"' || c == ';' || c == '\\') {
                throw new IllegalArgumentException("RFC6265 Cookie values may not contain character: [" + c + "]");
            }
            if (c >= '\u0080') {
                throw new IllegalArgumentException("RFC6265 Cookie values characters restricted to US-ASCII: 0x" + Integer.toHexString(c));
            }
            ++i;
        }
    }
}

