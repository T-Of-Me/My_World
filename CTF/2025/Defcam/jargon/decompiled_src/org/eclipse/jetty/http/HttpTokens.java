/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

public interface HttpTokens {
    public static final byte COLON = 58;
    public static final byte TAB = 9;
    public static final byte LINE_FEED = 10;
    public static final byte CARRIAGE_RETURN = 13;
    public static final byte SPACE = 32;
    public static final byte[] CRLF = new byte[]{13, 10};
    public static final byte SEMI_COLON = 59;

    public static enum EndOfContent {
        UNKNOWN_CONTENT,
        NO_CONTENT,
        EOF_CONTENT,
        CONTENT_LENGTH,
        CHUNKED_CONTENT;

    }
}

