/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.nio.ByteBuffer;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Trie;

public enum HttpScheme {
    HTTP("http"),
    HTTPS("https"),
    WS("ws"),
    WSS("wss");

    public static final Trie<HttpScheme> CACHE;
    private final String _string;
    private final ByteBuffer _buffer;

    private HttpScheme(String s) {
        this._string = s;
        this._buffer = BufferUtil.toBuffer(s);
    }

    public ByteBuffer asByteBuffer() {
        return this._buffer.asReadOnlyBuffer();
    }

    public boolean is(String s) {
        return s != null && this._string.equalsIgnoreCase(s);
    }

    public String asString() {
        return this._string;
    }

    public String toString() {
        return this._string;
    }

    static {
        CACHE = new ArrayTrie<HttpScheme>();
        for (HttpScheme version : HttpScheme.values()) {
            CACHE.put(version.asString(), version);
        }
    }
}

