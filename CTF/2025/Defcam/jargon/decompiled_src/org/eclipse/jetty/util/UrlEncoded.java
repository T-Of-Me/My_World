/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.Utf8Appendable;
import org.eclipse.jetty.util.Utf8StringBuffer;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class UrlEncoded
extends MultiMap<String>
implements Cloneable {
    static final Logger LOG;
    public static final Charset ENCODING;

    public UrlEncoded(UrlEncoded url) {
        super(url);
    }

    public UrlEncoded() {
    }

    public UrlEncoded(String query) {
        UrlEncoded.decodeTo(query, (MultiMap<String>)this, ENCODING);
    }

    public void decode(String query) {
        UrlEncoded.decodeTo(query, (MultiMap<String>)this, ENCODING);
    }

    public void decode(String query, Charset charset) {
        UrlEncoded.decodeTo(query, (MultiMap<String>)this, charset);
    }

    public String encode() {
        return this.encode(ENCODING, false);
    }

    public String encode(Charset charset) {
        return this.encode(charset, false);
    }

    public synchronized String encode(Charset charset, boolean equalsForNullValue) {
        return UrlEncoded.encode(this, charset, equalsForNullValue);
    }

    public static String encode(MultiMap<String> map, Charset charset, boolean equalsForNullValue) {
        if (charset == null) {
            charset = ENCODING;
        }
        StringBuilder result = new StringBuilder(128);
        boolean delim = false;
        for (Map.Entry entry : map.entrySet()) {
            String key = ((String)entry.getKey()).toString();
            List list = (List)entry.getValue();
            int s = list.size();
            if (delim) {
                result.append('&');
            }
            if (s == 0) {
                result.append(UrlEncoded.encodeString(key, charset));
                if (equalsForNullValue) {
                    result.append('=');
                }
            } else {
                for (int i = 0; i < s; ++i) {
                    if (i > 0) {
                        result.append('&');
                    }
                    String val = (String)list.get(i);
                    result.append(UrlEncoded.encodeString(key, charset));
                    if (val != null) {
                        String str = val.toString();
                        if (str.length() > 0) {
                            result.append('=');
                            result.append(UrlEncoded.encodeString(str, charset));
                            continue;
                        }
                        if (!equalsForNullValue) continue;
                        result.append('=');
                        continue;
                    }
                    if (!equalsForNullValue) continue;
                    result.append('=');
                }
            }
            delim = true;
        }
        return result.toString();
    }

    public static void decodeTo(String content, MultiMap<String> map, String charset) {
        UrlEncoded.decodeTo(content, map, charset == null ? null : Charset.forName(charset));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void decodeTo(String content, MultiMap<String> map, Charset charset) {
        if (charset == null) {
            charset = ENCODING;
        }
        if (charset == StandardCharsets.UTF_8) {
            UrlEncoded.decodeUtf8To(content, 0, content.length(), map);
            return;
        }
        MultiMap<String> multiMap = map;
        synchronized (multiMap) {
            String key = null;
            String value = null;
            int mark = -1;
            boolean encoded = false;
            block9: for (int i = 0; i < content.length(); ++i) {
                char c = content.charAt(i);
                switch (c) {
                    case '&': {
                        int l = i - mark - 1;
                        value = l == 0 ? "" : (encoded ? UrlEncoded.decodeString(content, mark + 1, l, charset) : content.substring(mark + 1, i));
                        mark = i;
                        encoded = false;
                        if (key != null) {
                            map.add(key, value);
                        } else if (value != null && value.length() > 0) {
                            map.add(value, "");
                        }
                        key = null;
                        value = null;
                        continue block9;
                    }
                    case '=': {
                        if (key != null) continue block9;
                        key = encoded ? UrlEncoded.decodeString(content, mark + 1, i - mark - 1, charset) : content.substring(mark + 1, i);
                        mark = i;
                        encoded = false;
                        continue block9;
                    }
                    case '+': {
                        encoded = true;
                        continue block9;
                    }
                    case '%': {
                        encoded = true;
                    }
                }
            }
            if (key != null) {
                int l = content.length() - mark - 1;
                value = l == 0 ? "" : (encoded ? UrlEncoded.decodeString(content, mark + 1, l, charset) : content.substring(mark + 1));
                map.add(key, value);
            } else if (mark < content.length()) {
                String string = key = encoded ? UrlEncoded.decodeString(content, mark + 1, content.length() - mark - 1, charset) : content.substring(mark + 1);
                if (key != null && key.length() > 0) {
                    map.add(key, "");
                }
            }
        }
    }

    public static void decodeUtf8To(String query, MultiMap<String> map) {
        UrlEncoded.decodeUtf8To(query, 0, query.length(), map);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void decodeUtf8To(String query, int offset, int length, MultiMap<String> map) {
        Utf8StringBuilder buffer = new Utf8StringBuilder();
        MultiMap<String> multiMap = map;
        synchronized (multiMap) {
            String key = null;
            String value = null;
            int end = offset + length;
            block9: for (int i = offset; i < end; ++i) {
                char c = query.charAt(i);
                switch (c) {
                    case '&': {
                        value = buffer.toReplacedString();
                        buffer.reset();
                        if (key != null) {
                            map.add(key, value);
                        } else if (value != null && value.length() > 0) {
                            map.add(value, "");
                        }
                        key = null;
                        value = null;
                        continue block9;
                    }
                    case '=': {
                        if (key != null) {
                            buffer.append(c);
                            continue block9;
                        }
                        key = buffer.toReplacedString();
                        buffer.reset();
                        continue block9;
                    }
                    case '+': {
                        buffer.append((byte)32);
                        continue block9;
                    }
                    case '%': {
                        if (i + 2 < end) {
                            char hi = query.charAt(++i);
                            char lo = query.charAt(++i);
                            buffer.append(UrlEncoded.decodeHexByte(hi, lo));
                            continue block9;
                        }
                        throw new Utf8Appendable.NotUtf8Exception("Incomplete % encoding");
                    }
                    default: {
                        buffer.append(c);
                    }
                }
            }
            if (key != null) {
                value = buffer.toReplacedString();
                buffer.reset();
                map.add(key, value);
            } else if (buffer.length() > 0) {
                map.add(buffer.toReplacedString(), "");
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void decode88591To(InputStream in, MultiMap<String> map, int maxLength, int maxKeys) throws IOException {
        MultiMap<String> multiMap = map;
        synchronized (multiMap) {
            int b;
            StringBuffer buffer = new StringBuffer();
            String key = null;
            String value = null;
            int totalLength = 0;
            while ((b = in.read()) >= 0) {
                switch ((char)b) {
                    case '&': {
                        value = buffer.length() == 0 ? "" : buffer.toString();
                        buffer.setLength(0);
                        if (key != null) {
                            map.add(key, value);
                        } else if (value != null && value.length() > 0) {
                            map.add(value, "");
                        }
                        key = null;
                        value = null;
                        if (maxKeys <= 0 || map.size() <= maxKeys) break;
                        throw new IllegalStateException(String.format("Form with too many keys [%d > %d]", map.size(), maxKeys));
                    }
                    case '=': {
                        if (key != null) {
                            buffer.append((char)b);
                            break;
                        }
                        key = buffer.toString();
                        buffer.setLength(0);
                        break;
                    }
                    case '+': {
                        buffer.append(' ');
                        break;
                    }
                    case '%': {
                        int code0 = in.read();
                        int code1 = in.read();
                        buffer.append(UrlEncoded.decodeHexChar(code0, code1));
                        break;
                    }
                    default: {
                        buffer.append((char)b);
                    }
                }
                if (maxLength < 0 || ++totalLength <= maxLength) continue;
                throw new IllegalStateException(String.format("Form with too many keys [%d > %d]", map.size(), maxKeys));
            }
            if (key != null) {
                value = buffer.length() == 0 ? "" : buffer.toString();
                buffer.setLength(0);
                map.add(key, value);
            } else if (buffer.length() > 0) {
                map.add(buffer.toString(), "");
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void decodeUtf8To(InputStream in, MultiMap<String> map, int maxLength, int maxKeys) throws IOException {
        MultiMap<String> multiMap = map;
        synchronized (multiMap) {
            int b;
            Utf8StringBuilder buffer = new Utf8StringBuilder();
            String key = null;
            String value = null;
            int totalLength = 0;
            while ((b = in.read()) >= 0) {
                switch ((char)b) {
                    case '&': {
                        value = buffer.toReplacedString();
                        buffer.reset();
                        if (key != null) {
                            map.add(key, value);
                        } else if (value != null && value.length() > 0) {
                            map.add(value, "");
                        }
                        key = null;
                        value = null;
                        if (maxKeys <= 0 || map.size() <= maxKeys) break;
                        throw new IllegalStateException(String.format("Form with too many keys [%d > %d]", map.size(), maxKeys));
                    }
                    case '=': {
                        if (key != null) {
                            buffer.append((byte)b);
                            break;
                        }
                        key = buffer.toReplacedString();
                        buffer.reset();
                        break;
                    }
                    case '+': {
                        buffer.append((byte)32);
                        break;
                    }
                    case '%': {
                        char code0 = (char)in.read();
                        char code1 = (char)in.read();
                        buffer.append(UrlEncoded.decodeHexByte(code0, code1));
                        break;
                    }
                    default: {
                        buffer.append((byte)b);
                    }
                }
                if (maxLength < 0 || ++totalLength <= maxLength) continue;
                throw new IllegalStateException("Form is too large");
            }
            if (key != null) {
                value = buffer.toReplacedString();
                buffer.reset();
                map.add(key, value);
            } else if (buffer.length() > 0) {
                map.add(buffer.toReplacedString(), "");
            }
        }
    }

    public static void decodeUtf16To(InputStream in, MultiMap<String> map, int maxLength, int maxKeys) throws IOException {
        InputStreamReader input = new InputStreamReader(in, StandardCharsets.UTF_16);
        StringWriter buf = new StringWriter(8192);
        IO.copy(input, buf, (long)maxLength);
        UrlEncoded.decodeTo(buf.getBuffer().toString(), map, StandardCharsets.UTF_16);
    }

    public static void decodeTo(InputStream in, MultiMap<String> map, String charset, int maxLength, int maxKeys) throws IOException {
        if (charset == null) {
            if (ENCODING.equals(StandardCharsets.UTF_8)) {
                UrlEncoded.decodeUtf8To(in, map, maxLength, maxKeys);
            } else {
                UrlEncoded.decodeTo(in, map, ENCODING, maxLength, maxKeys);
            }
        } else if ("utf-8".equalsIgnoreCase(charset)) {
            UrlEncoded.decodeUtf8To(in, map, maxLength, maxKeys);
        } else if ("iso-8859-1".equalsIgnoreCase(charset)) {
            UrlEncoded.decode88591To(in, map, maxLength, maxKeys);
        } else if ("utf-16".equalsIgnoreCase(charset)) {
            UrlEncoded.decodeUtf16To(in, map, maxLength, maxKeys);
        } else {
            UrlEncoded.decodeTo(in, map, Charset.forName(charset), maxLength, maxKeys);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void decodeTo(InputStream in, MultiMap<String> map, Charset charset, int maxLength, int maxKeys) throws IOException {
        if (charset == null) {
            charset = ENCODING;
        }
        if (StandardCharsets.UTF_8.equals(charset)) {
            UrlEncoded.decodeUtf8To(in, map, maxLength, maxKeys);
            return;
        }
        if (StandardCharsets.ISO_8859_1.equals(charset)) {
            UrlEncoded.decode88591To(in, map, maxLength, maxKeys);
            return;
        }
        if (StandardCharsets.UTF_16.equals(charset)) {
            UrlEncoded.decodeUtf16To(in, map, maxLength, maxKeys);
            return;
        }
        MultiMap<String> multiMap = map;
        synchronized (multiMap) {
            String key = null;
            String value = null;
            int totalLength = 0;
            try (ByteArrayOutputStream2 output = new ByteArrayOutputStream2();){
                int c;
                int size = 0;
                while ((c = in.read()) > 0) {
                    switch ((char)c) {
                        case '&': {
                            size = output.size();
                            value = size == 0 ? "" : output.toString(charset);
                            output.setCount(0);
                            if (key != null) {
                                map.add(key, value);
                            } else if (value != null && value.length() > 0) {
                                map.add(value, "");
                            }
                            key = null;
                            value = null;
                            if (maxKeys <= 0 || map.size() <= maxKeys) break;
                            throw new IllegalStateException(String.format("Form with too many keys [%d > %d]", map.size(), maxKeys));
                        }
                        case '=': {
                            if (key != null) {
                                output.write(c);
                                break;
                            }
                            size = output.size();
                            key = size == 0 ? "" : output.toString(charset);
                            output.setCount(0);
                            break;
                        }
                        case '+': {
                            output.write(32);
                            break;
                        }
                        case '%': {
                            int code0 = in.read();
                            int code1 = in.read();
                            output.write(UrlEncoded.decodeHexChar(code0, code1));
                            break;
                        }
                        default: {
                            output.write(c);
                        }
                    }
                    if (maxLength < 0 || ++totalLength <= maxLength) continue;
                    throw new IllegalStateException("Form is too large");
                }
                size = output.size();
                if (key != null) {
                    value = size == 0 ? "" : output.toString(charset);
                    output.setCount(0);
                    map.add(key, value);
                } else if (size > 0) {
                    map.add(output.toString(charset), "");
                }
            }
        }
    }

    public static String decodeString(String encoded) {
        return UrlEncoded.decodeString(encoded, 0, encoded.length(), ENCODING);
    }

    public static String decodeString(String encoded, int offset, int length, Charset charset) {
        if (charset == null || StandardCharsets.UTF_8.equals(charset)) {
            Utf8Appendable buffer = null;
            for (int i = 0; i < length; ++i) {
                char c = encoded.charAt(offset + i);
                if (c < '\u0000' || c > '\u00ff') {
                    if (buffer == null) {
                        buffer = new Utf8StringBuffer(length);
                        ((Utf8StringBuffer)buffer).getStringBuffer().append(encoded, offset, offset + i + 1);
                        continue;
                    }
                    ((Utf8StringBuffer)buffer).getStringBuffer().append(c);
                    continue;
                }
                if (c == '+') {
                    if (buffer == null) {
                        buffer = new Utf8StringBuffer(length);
                        ((Utf8StringBuffer)buffer).getStringBuffer().append(encoded, offset, offset + i);
                    }
                    ((Utf8StringBuffer)buffer).getStringBuffer().append(' ');
                    continue;
                }
                if (c == '%') {
                    if (buffer == null) {
                        buffer = new Utf8StringBuffer(length);
                        ((Utf8StringBuffer)buffer).getStringBuffer().append(encoded, offset, offset + i);
                    }
                    if (i + 2 < length) {
                        int o = offset + i + 1;
                        i += 2;
                        byte b = (byte)TypeUtil.parseInt(encoded, o, 2, 16);
                        buffer.append(b);
                        continue;
                    }
                    ((Utf8StringBuffer)buffer).getStringBuffer().append('\ufffd');
                    i = length;
                    continue;
                }
                if (buffer == null) continue;
                ((Utf8StringBuffer)buffer).getStringBuffer().append(c);
            }
            if (buffer == null) {
                if (offset == 0 && encoded.length() == length) {
                    return encoded;
                }
                return encoded.substring(offset, offset + length);
            }
            return buffer.toReplacedString();
        }
        StringBuffer buffer = null;
        for (int i = 0; i < length; ++i) {
            char c = encoded.charAt(offset + i);
            if (c < '\u0000' || c > '\u00ff') {
                if (buffer == null) {
                    buffer = new StringBuffer(length);
                    buffer.append(encoded, offset, offset + i + 1);
                    continue;
                }
                buffer.append(c);
                continue;
            }
            if (c == '+') {
                if (buffer == null) {
                    buffer = new StringBuffer(length);
                    buffer.append(encoded, offset, offset + i);
                }
                buffer.append(' ');
                continue;
            }
            if (c == '%') {
                if (buffer == null) {
                    buffer = new StringBuffer(length);
                    buffer.append(encoded, offset, offset + i);
                }
                byte[] ba = new byte[length];
                int n = 0;
                while (c >= '\u0000' && c <= '\u00ff') {
                    if (c == '%') {
                        if (i + 2 < length) {
                            int o = offset + i + 1;
                            i += 3;
                            ba[n] = (byte)TypeUtil.parseInt(encoded, o, 2, 16);
                            ++n;
                        } else {
                            ba[n++] = 63;
                            i = length;
                        }
                    } else if (c == '+') {
                        ba[n++] = 32;
                        ++i;
                    } else {
                        ba[n++] = (byte)c;
                        ++i;
                    }
                    if (i >= length) break;
                    c = encoded.charAt(offset + i);
                }
                --i;
                buffer.append(new String(ba, 0, n, charset));
                continue;
            }
            if (buffer == null) continue;
            buffer.append(c);
        }
        if (buffer == null) {
            if (offset == 0 && encoded.length() == length) {
                return encoded;
            }
            return encoded.substring(offset, offset + length);
        }
        return buffer.toString();
    }

    private static char decodeHexChar(int hi, int lo) {
        try {
            return (char)((TypeUtil.convertHexDigit(hi) << 4) + TypeUtil.convertHexDigit(lo));
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not valid encoding '%" + (char)hi + (char)lo + "'");
        }
    }

    private static byte decodeHexByte(char hi, char lo) {
        try {
            return (byte)((TypeUtil.convertHexDigit(hi) << 4) + TypeUtil.convertHexDigit(lo));
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not valid encoding '%" + hi + lo + "'");
        }
    }

    public static String encodeString(String string) {
        return UrlEncoded.encodeString(string, ENCODING);
    }

    public static String encodeString(String string, Charset charset) {
        if (charset == null) {
            charset = ENCODING;
        }
        byte[] bytes = null;
        bytes = string.getBytes(charset);
        int len = bytes.length;
        byte[] encoded = new byte[bytes.length * 3];
        int n = 0;
        boolean noEncode = true;
        for (int i = 0; i < len; ++i) {
            byte b = bytes[i];
            if (b == 32) {
                noEncode = false;
                encoded[n++] = 43;
                continue;
            }
            if (b >= 97 && b <= 122 || b >= 65 && b <= 90 || b >= 48 && b <= 57) {
                encoded[n++] = b;
                continue;
            }
            noEncode = false;
            encoded[n++] = 37;
            byte nibble = (byte)((b & 0xF0) >> 4);
            encoded[n++] = nibble >= 10 ? (byte)(65 + nibble - 10) : (byte)(48 + nibble);
            nibble = (byte)(b & 0xF);
            encoded[n++] = nibble >= 10 ? (byte)(65 + nibble - 10) : (byte)(48 + nibble);
        }
        if (noEncode) {
            return string;
        }
        return new String(encoded, 0, n, charset);
    }

    @Override
    public Object clone() {
        return new UrlEncoded(this);
    }

    static {
        Charset encoding;
        LOG = Log.getLogger(UrlEncoded.class);
        try {
            String charset = System.getProperty("org.eclipse.jetty.util.UrlEncoding.charset");
            encoding = charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
        }
        catch (Exception e) {
            LOG.warn(e);
            encoding = StandardCharsets.UTF_8;
        }
        ENCODING = encoding;
    }
}

