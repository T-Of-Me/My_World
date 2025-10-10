/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.nio.ByteBuffer;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Trie;

public enum HttpMethod {
    GET,
    POST,
    HEAD,
    PUT,
    OPTIONS,
    DELETE,
    TRACE,
    CONNECT,
    MOVE,
    PROXY,
    PRI;

    public static final Trie<HttpMethod> CACHE;
    private final ByteBuffer _buffer;
    private final byte[] _bytes = StringUtil.getBytes(this.toString());

    public static HttpMethod lookAheadGet(byte[] bytes, int position, int limit) {
        int length = limit - position;
        if (length < 4) {
            return null;
        }
        switch (bytes[position]) {
            case 71: {
                if (bytes[position + 1] != 69 || bytes[position + 2] != 84 || bytes[position + 3] != 32) break;
                return GET;
            }
            case 80: {
                if (bytes[position + 1] == 79 && bytes[position + 2] == 83 && bytes[position + 3] == 84 && length >= 5 && bytes[position + 4] == 32) {
                    return POST;
                }
                if (bytes[position + 1] == 82 && bytes[position + 2] == 79 && bytes[position + 3] == 88 && length >= 6 && bytes[position + 4] == 89 && bytes[position + 5] == 32) {
                    return PROXY;
                }
                if (bytes[position + 1] == 85 && bytes[position + 2] == 84 && bytes[position + 3] == 32) {
                    return PUT;
                }
                if (bytes[position + 1] != 82 || bytes[position + 2] != 73 || bytes[position + 3] != 32) break;
                return PRI;
            }
            case 72: {
                if (bytes[position + 1] != 69 || bytes[position + 2] != 65 || bytes[position + 3] != 68 || length < 5 || bytes[position + 4] != 32) break;
                return HEAD;
            }
            case 79: {
                if (bytes[position + 1] != 80 || bytes[position + 2] != 84 || bytes[position + 3] != 73 || length < 8 || bytes[position + 4] != 79 || bytes[position + 5] != 78 || bytes[position + 6] != 83 || bytes[position + 7] != 32) break;
                return OPTIONS;
            }
            case 68: {
                if (bytes[position + 1] != 69 || bytes[position + 2] != 76 || bytes[position + 3] != 69 || length < 7 || bytes[position + 4] != 84 || bytes[position + 5] != 69 || bytes[position + 6] != 32) break;
                return DELETE;
            }
            case 84: {
                if (bytes[position + 1] != 82 || bytes[position + 2] != 65 || bytes[position + 3] != 67 || length < 6 || bytes[position + 4] != 69 || bytes[position + 5] != 32) break;
                return TRACE;
            }
            case 67: {
                if (bytes[position + 1] != 79 || bytes[position + 2] != 78 || bytes[position + 3] != 78 || length < 8 || bytes[position + 4] != 69 || bytes[position + 5] != 67 || bytes[position + 6] != 84 || bytes[position + 7] != 32) break;
                return CONNECT;
            }
            case 77: {
                if (bytes[position + 1] != 79 || bytes[position + 2] != 86 || bytes[position + 3] != 69 || length < 5 || bytes[position + 4] != 32) break;
                return MOVE;
            }
        }
        return null;
    }

    public static HttpMethod lookAheadGet(ByteBuffer buffer) {
        int ml;
        HttpMethod m;
        if (buffer.hasArray()) {
            return HttpMethod.lookAheadGet(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.arrayOffset() + buffer.limit());
        }
        int l = buffer.remaining();
        if (l >= 4 && (m = CACHE.getBest(buffer, 0, l)) != null && l > (ml = m.asString().length()) && buffer.get(buffer.position() + ml) == 32) {
            return m;
        }
        return null;
    }

    private HttpMethod() {
        this._buffer = ByteBuffer.wrap(this._bytes);
    }

    public byte[] getBytes() {
        return this._bytes;
    }

    public boolean is(String s) {
        return this.toString().equalsIgnoreCase(s);
    }

    public ByteBuffer asBuffer() {
        return this._buffer.asReadOnlyBuffer();
    }

    public String asString() {
        return this.toString();
    }

    public static HttpMethod fromString(String method) {
        return CACHE.get(method);
    }

    static {
        CACHE = new ArrayTrie<HttpMethod>();
        for (HttpMethod method : HttpMethod.values()) {
            CACHE.put(method.toString(), method);
        }
    }
}

