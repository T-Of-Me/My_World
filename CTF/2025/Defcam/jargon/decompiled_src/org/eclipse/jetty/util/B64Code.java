/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class B64Code {
    private static final char __pad = '=';
    private static final char[] __rfc1421alphabet;
    private static final byte[] __rfc1421nibbles;
    private static final char[] __rfc4648urlAlphabet;
    private static final byte[] __rfc4648urlNibbles;

    private B64Code() {
    }

    public static String encode(String s) {
        return B64Code.encode(s, (Charset)null);
    }

    public static String encode(String s, String charEncoding) {
        byte[] bytes = charEncoding == null ? s.getBytes(StandardCharsets.ISO_8859_1) : s.getBytes(Charset.forName(charEncoding));
        return new String(B64Code.encode(bytes));
    }

    public static String encode(String s, Charset charEncoding) {
        byte[] bytes = s.getBytes(charEncoding == null ? StandardCharsets.ISO_8859_1 : charEncoding);
        return new String(B64Code.encode(bytes));
    }

    public static char[] encode(byte[] b) {
        byte b1;
        byte b0;
        if (b == null) {
            return null;
        }
        int bLen = b.length;
        int cLen = (bLen + 2) / 3 * 4;
        char[] c = new char[cLen];
        int ci = 0;
        int bi = 0;
        int stop = bLen / 3 * 3;
        while (bi < stop) {
            b0 = b[bi++];
            b1 = b[bi++];
            byte b2 = b[bi++];
            c[ci++] = __rfc1421alphabet[b0 >>> 2 & 0x3F];
            c[ci++] = __rfc1421alphabet[b0 << 4 & 0x3F | b1 >>> 4 & 0xF];
            c[ci++] = __rfc1421alphabet[b1 << 2 & 0x3F | b2 >>> 6 & 3];
            c[ci++] = __rfc1421alphabet[b2 & 0x3F];
        }
        if (bLen != bi) {
            switch (bLen % 3) {
                case 2: {
                    b0 = b[bi++];
                    b1 = b[bi++];
                    c[ci++] = __rfc1421alphabet[b0 >>> 2 & 0x3F];
                    c[ci++] = __rfc1421alphabet[b0 << 4 & 0x3F | b1 >>> 4 & 0xF];
                    c[ci++] = __rfc1421alphabet[b1 << 2 & 0x3F];
                    c[ci++] = 61;
                    break;
                }
                case 1: {
                    b0 = b[bi++];
                    c[ci++] = __rfc1421alphabet[b0 >>> 2 & 0x3F];
                    c[ci++] = __rfc1421alphabet[b0 << 4 & 0x3F];
                    c[ci++] = 61;
                    c[ci++] = 61;
                    break;
                }
            }
        }
        return c;
    }

    public static char[] encode(byte[] b, boolean rfc2045) {
        byte b1;
        byte b0;
        if (b == null) {
            return null;
        }
        if (!rfc2045) {
            return B64Code.encode(b);
        }
        int bLen = b.length;
        int cLen = (bLen + 2) / 3 * 4;
        cLen += 2 + 2 * (cLen / 76);
        char[] c = new char[cLen];
        int ci = 0;
        int bi = 0;
        int stop = bLen / 3 * 3;
        int l = 0;
        while (bi < stop) {
            b0 = b[bi++];
            b1 = b[bi++];
            byte b2 = b[bi++];
            c[ci++] = __rfc1421alphabet[b0 >>> 2 & 0x3F];
            c[ci++] = __rfc1421alphabet[b0 << 4 & 0x3F | b1 >>> 4 & 0xF];
            c[ci++] = __rfc1421alphabet[b1 << 2 & 0x3F | b2 >>> 6 & 3];
            c[ci++] = __rfc1421alphabet[b2 & 0x3F];
            if ((l += 4) % 76 != 0) continue;
            c[ci++] = 13;
            c[ci++] = 10;
        }
        if (bLen != bi) {
            switch (bLen % 3) {
                case 2: {
                    b0 = b[bi++];
                    b1 = b[bi++];
                    c[ci++] = __rfc1421alphabet[b0 >>> 2 & 0x3F];
                    c[ci++] = __rfc1421alphabet[b0 << 4 & 0x3F | b1 >>> 4 & 0xF];
                    c[ci++] = __rfc1421alphabet[b1 << 2 & 0x3F];
                    c[ci++] = 61;
                    break;
                }
                case 1: {
                    b0 = b[bi++];
                    c[ci++] = __rfc1421alphabet[b0 >>> 2 & 0x3F];
                    c[ci++] = __rfc1421alphabet[b0 << 4 & 0x3F];
                    c[ci++] = 61;
                    c[ci++] = 61;
                    break;
                }
            }
        }
        c[ci++] = 13;
        c[ci++] = 10;
        return c;
    }

    public static String decode(String encoded, String charEncoding) {
        byte[] decoded = B64Code.decode(encoded);
        if (charEncoding == null) {
            return new String(decoded);
        }
        return new String(decoded, Charset.forName(charEncoding));
    }

    public static String decode(String encoded, Charset charEncoding) {
        byte[] decoded = B64Code.decode(encoded);
        if (charEncoding == null) {
            return new String(decoded);
        }
        return new String(decoded, charEncoding);
    }

    public static byte[] decode(char[] b) {
        int li;
        if (b == null) {
            return null;
        }
        int bLen = b.length;
        if (bLen % 4 != 0) {
            throw new IllegalArgumentException("Input block size is not 4");
        }
        for (li = bLen - 1; li >= 0 && b[li] == '='; --li) {
        }
        if (li < 0) {
            return new byte[0];
        }
        int rLen = (li + 1) * 3 / 4;
        byte[] r = new byte[rLen];
        int ri = 0;
        int bi = 0;
        int stop = rLen / 3 * 3;
        try {
            byte b2;
            byte b1;
            byte b0;
            while (ri < stop) {
                b0 = __rfc1421nibbles[b[bi++]];
                b1 = __rfc1421nibbles[b[bi++]];
                b2 = __rfc1421nibbles[b[bi++]];
                byte b3 = __rfc1421nibbles[b[bi++]];
                if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                    throw new IllegalArgumentException("Not B64 encoded");
                }
                r[ri++] = (byte)(b0 << 2 | b1 >>> 4);
                r[ri++] = (byte)(b1 << 4 | b2 >>> 2);
                r[ri++] = (byte)(b2 << 6 | b3);
            }
            if (rLen != ri) {
                switch (rLen % 3) {
                    case 2: {
                        b0 = __rfc1421nibbles[b[bi++]];
                        b1 = __rfc1421nibbles[b[bi++]];
                        b2 = __rfc1421nibbles[b[bi++]];
                        if (b0 < 0 || b1 < 0 || b2 < 0) {
                            throw new IllegalArgumentException("Not B64 encoded");
                        }
                        r[ri++] = (byte)(b0 << 2 | b1 >>> 4);
                        r[ri++] = (byte)(b1 << 4 | b2 >>> 2);
                        break;
                    }
                    case 1: {
                        b0 = __rfc1421nibbles[b[bi++]];
                        b1 = __rfc1421nibbles[b[bi++]];
                        if (b0 < 0 || b1 < 0) {
                            throw new IllegalArgumentException("Not B64 encoded");
                        }
                        r[ri++] = (byte)(b0 << 2 | b1 >>> 4);
                        break;
                    }
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("char " + bi + " was not B64 encoded");
        }
        return r;
    }

    public static byte[] decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream(4 * encoded.length() / 3);
        B64Code.decode(encoded, bout);
        return bout.toByteArray();
    }

    public static void decode(String encoded, ByteArrayOutputStream bout) {
        char c;
        if (encoded == null) {
            return;
        }
        if (bout == null) {
            throw new IllegalArgumentException("No outputstream for decoded bytes");
        }
        int ci = 0;
        byte[] nibbles = new byte[4];
        int s = 0;
        while (ci < encoded.length() && (c = encoded.charAt(ci++)) != '=') {
            if (Character.isWhitespace(c)) continue;
            byte nibble = __rfc1421nibbles[c];
            if (nibble < 0) {
                throw new IllegalArgumentException("Not B64 encoded");
            }
            nibbles[s++] = __rfc1421nibbles[c];
            switch (s) {
                case 1: {
                    break;
                }
                case 2: {
                    bout.write(nibbles[0] << 2 | nibbles[1] >>> 4);
                    break;
                }
                case 3: {
                    bout.write(nibbles[1] << 4 | nibbles[2] >>> 2);
                    break;
                }
                case 4: {
                    bout.write(nibbles[2] << 6 | nibbles[3]);
                    s = 0;
                }
            }
        }
    }

    public static byte[] decodeRFC4648URL(String encoded) {
        if (encoded == null) {
            return null;
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream(4 * encoded.length() / 3);
        B64Code.decodeRFC4648URL(encoded, bout);
        return bout.toByteArray();
    }

    public static void decodeRFC4648URL(String encoded, ByteArrayOutputStream bout) {
        char c;
        if (encoded == null) {
            return;
        }
        if (bout == null) {
            throw new IllegalArgumentException("No outputstream for decoded bytes");
        }
        int ci = 0;
        byte[] nibbles = new byte[4];
        int s = 0;
        while (ci < encoded.length() && (c = encoded.charAt(ci++)) != '=') {
            if (Character.isWhitespace(c)) continue;
            byte nibble = __rfc4648urlNibbles[c];
            if (nibble < 0) {
                throw new IllegalArgumentException("Not B64 encoded");
            }
            nibbles[s++] = __rfc4648urlNibbles[c];
            switch (s) {
                case 1: {
                    break;
                }
                case 2: {
                    bout.write(nibbles[0] << 2 | nibbles[1] >>> 4);
                    break;
                }
                case 3: {
                    bout.write(nibbles[1] << 4 | nibbles[2] >>> 2);
                    break;
                }
                case 4: {
                    bout.write(nibbles[2] << 6 | nibbles[3]);
                    s = 0;
                }
            }
        }
    }

    public static void encode(int value, Appendable buf) throws IOException {
        buf.append(__rfc1421alphabet[0x3F & (0xFC000000 & value) >> 26]);
        buf.append(__rfc1421alphabet[0x3F & (0x3F00000 & value) >> 20]);
        buf.append(__rfc1421alphabet[0x3F & (0xFC000 & value) >> 14]);
        buf.append(__rfc1421alphabet[0x3F & (0x3F00 & value) >> 8]);
        buf.append(__rfc1421alphabet[0x3F & (0xFC & value) >> 2]);
        buf.append(__rfc1421alphabet[0x3F & (3 & value) << 4]);
        buf.append('=');
    }

    public static void encode(long lvalue, Appendable buf) throws IOException {
        int value = (int)(0xFFFFFFFFFFFFFFFCL & lvalue >> 32);
        buf.append(__rfc1421alphabet[0x3F & (0xFC000000 & value) >> 26]);
        buf.append(__rfc1421alphabet[0x3F & (0x3F00000 & value) >> 20]);
        buf.append(__rfc1421alphabet[0x3F & (0xFC000 & value) >> 14]);
        buf.append(__rfc1421alphabet[0x3F & (0x3F00 & value) >> 8]);
        buf.append(__rfc1421alphabet[0x3F & (0xFC & value) >> 2]);
        buf.append(__rfc1421alphabet[0x3F & ((3 & value) << 4) + (0xF & (int)(lvalue >> 28))]);
        value = 0xFFFFFFF & (int)lvalue;
        buf.append(__rfc1421alphabet[0x3F & (0xFC00000 & value) >> 22]);
        buf.append(__rfc1421alphabet[0x3F & (0x3F0000 & value) >> 16]);
        buf.append(__rfc1421alphabet[0x3F & (0xFC00 & value) >> 10]);
        buf.append(__rfc1421alphabet[0x3F & (0x3F0 & value) >> 4]);
        buf.append(__rfc1421alphabet[0x3F & (0xF & value) << 2]);
    }

    static {
        int b;
        int i;
        __rfc1421alphabet = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
        __rfc1421nibbles = new byte[256];
        for (i = 0; i < 256; ++i) {
            B64Code.__rfc1421nibbles[i] = -1;
        }
        for (b = 0; b < 64; b = (int)((byte)(b + 1))) {
            B64Code.__rfc1421nibbles[(byte)B64Code.__rfc1421alphabet[b]] = b;
        }
        B64Code.__rfc1421nibbles[61] = 0;
        __rfc4648urlAlphabet = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
        __rfc4648urlNibbles = new byte[256];
        for (i = 0; i < 256; ++i) {
            B64Code.__rfc4648urlNibbles[i] = -1;
        }
        for (b = 0; b < 64; b = (int)((byte)(b + 1))) {
            B64Code.__rfc4648urlNibbles[(byte)B64Code.__rfc4648urlAlphabet[b]] = b;
        }
        B64Code.__rfc4648urlNibbles[61] = 0;
    }
}

