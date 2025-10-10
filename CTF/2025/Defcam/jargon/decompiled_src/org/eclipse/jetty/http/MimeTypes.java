/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.PreEncodedHttpField;
import org.eclipse.jetty.util.ArrayTrie;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class MimeTypes {
    private static final Logger LOG;
    private static final Trie<ByteBuffer> TYPES;
    private static final Map<String, String> __dftMimeMap;
    private static final Map<String, String> __inferredEncodings;
    private static final Map<String, String> __assumedEncodings;
    public static final Trie<Type> CACHE;
    private final Map<String, String> _mimeMap = new HashMap<String, String>();

    public synchronized Map<String, String> getMimeMap() {
        return this._mimeMap;
    }

    public void setMimeMap(Map<String, String> mimeMap) {
        this._mimeMap.clear();
        if (mimeMap != null) {
            for (Map.Entry<String, String> ext : mimeMap.entrySet()) {
                this._mimeMap.put(StringUtil.asciiToLowerCase(ext.getKey()), MimeTypes.normalizeMimeType(ext.getValue()));
            }
        }
    }

    public static String getDefaultMimeByExtension(String filename) {
        String type = null;
        if (filename != null) {
            int i = -1;
            while (type == null && (i = filename.indexOf(".", i + 1)) >= 0 && i < filename.length()) {
                String ext = StringUtil.asciiToLowerCase(filename.substring(i + 1));
                if (type != null) continue;
                type = __dftMimeMap.get(ext);
            }
        }
        if (type == null && type == null) {
            type = __dftMimeMap.get("*");
        }
        return type;
    }

    public String getMimeByExtension(String filename) {
        String type = null;
        if (filename != null) {
            int i = -1;
            while (type == null && (i = filename.indexOf(".", i + 1)) >= 0 && i < filename.length()) {
                String ext = StringUtil.asciiToLowerCase(filename.substring(i + 1));
                if (this._mimeMap != null) {
                    type = this._mimeMap.get(ext);
                }
                if (type != null) continue;
                type = __dftMimeMap.get(ext);
            }
        }
        if (type == null) {
            if (this._mimeMap != null) {
                type = this._mimeMap.get("*");
            }
            if (type == null) {
                type = __dftMimeMap.get("*");
            }
        }
        return type;
    }

    public void addMimeMapping(String extension, String type) {
        this._mimeMap.put(StringUtil.asciiToLowerCase(extension), MimeTypes.normalizeMimeType(type));
    }

    public static Set<String> getKnownMimeTypes() {
        return new HashSet<String>(__dftMimeMap.values());
    }

    private static String normalizeMimeType(String type) {
        Type t = CACHE.get(type);
        if (t != null) {
            return t.asString();
        }
        return StringUtil.asciiToLowerCase(type);
    }

    public static String getCharsetFromContentType(String value) {
        int i;
        if (value == null) {
            return null;
        }
        int end = value.length();
        int state = 0;
        int start = 0;
        boolean quote = false;
        block13: for (i = 0; i < end; ++i) {
            char b = value.charAt(i);
            if (quote && state != 10) {
                if ('\"' != b) continue;
                quote = false;
                continue;
            }
            if (';' == b && state <= 8) {
                state = 1;
                continue;
            }
            switch (state) {
                case 0: {
                    if ('\"' != b) continue block13;
                    quote = true;
                    continue block13;
                }
                case 1: {
                    if ('c' == b) {
                        state = 2;
                        continue block13;
                    }
                    if (' ' == b) continue block13;
                    state = 0;
                    continue block13;
                }
                case 2: {
                    if ('h' == b) {
                        state = 3;
                        continue block13;
                    }
                    state = 0;
                    continue block13;
                }
                case 3: {
                    if ('a' == b) {
                        state = 4;
                        continue block13;
                    }
                    state = 0;
                    continue block13;
                }
                case 4: {
                    if ('r' == b) {
                        state = 5;
                        continue block13;
                    }
                    state = 0;
                    continue block13;
                }
                case 5: {
                    if ('s' == b) {
                        state = 6;
                        continue block13;
                    }
                    state = 0;
                    continue block13;
                }
                case 6: {
                    if ('e' == b) {
                        state = 7;
                        continue block13;
                    }
                    state = 0;
                    continue block13;
                }
                case 7: {
                    if ('t' == b) {
                        state = 8;
                        continue block13;
                    }
                    state = 0;
                    continue block13;
                }
                case 8: {
                    if ('=' == b) {
                        state = 9;
                        continue block13;
                    }
                    if (' ' == b) continue block13;
                    state = 0;
                    continue block13;
                }
                case 9: {
                    if (' ' == b) continue block13;
                    if ('\"' == b) {
                        quote = true;
                        start = i + 1;
                        state = 10;
                        continue block13;
                    }
                    start = i;
                    state = 10;
                    continue block13;
                }
                case 10: {
                    if ((quote || ';' != b && ' ' != b) && (!quote || '\"' != b)) continue block13;
                    return StringUtil.normalizeCharset(value, start, i - start);
                }
            }
        }
        if (state == 10) {
            return StringUtil.normalizeCharset(value, start, i - start);
        }
        return null;
    }

    public static Map<String, String> getInferredEncodings() {
        return __inferredEncodings;
    }

    public static Map<String, String> getAssumedEncodings() {
        return __inferredEncodings;
    }

    @Deprecated
    public static String inferCharsetFromContentType(String contentType) {
        return MimeTypes.getCharsetAssumedFromContentType(contentType);
    }

    public static String getCharsetInferredFromContentType(String contentType) {
        return __inferredEncodings.get(contentType);
    }

    public static String getCharsetAssumedFromContentType(String contentType) {
        return __assumedEncodings.get(contentType);
    }

    public static String getContentTypeWithoutCharset(String value) {
        int end = value.length();
        int state = 0;
        int start = 0;
        boolean quote = false;
        StringBuilder builder = null;
        block19: for (int i = 0; i < end; ++i) {
            char b = value.charAt(i);
            if ('\"' == b) {
                quote = !quote;
                switch (state) {
                    case 11: {
                        builder.append(b);
                        break;
                    }
                    case 10: {
                        break;
                    }
                    case 9: {
                        builder = new StringBuilder();
                        builder.append(value, 0, start + 1);
                        state = 10;
                        break;
                    }
                    default: {
                        start = i;
                        state = 0;
                        break;
                    }
                }
                continue;
            }
            if (quote) {
                if (builder == null || state == 10) continue;
                builder.append(b);
                continue;
            }
            switch (state) {
                case 0: {
                    if (';' == b) {
                        state = 1;
                        continue block19;
                    }
                    if (' ' == b) continue block19;
                    start = i;
                    continue block19;
                }
                case 1: {
                    if ('c' == b) {
                        state = 2;
                        continue block19;
                    }
                    if (' ' == b) continue block19;
                    state = 0;
                    continue block19;
                }
                case 2: {
                    if ('h' == b) {
                        state = 3;
                        continue block19;
                    }
                    state = 0;
                    continue block19;
                }
                case 3: {
                    if ('a' == b) {
                        state = 4;
                        continue block19;
                    }
                    state = 0;
                    continue block19;
                }
                case 4: {
                    if ('r' == b) {
                        state = 5;
                        continue block19;
                    }
                    state = 0;
                    continue block19;
                }
                case 5: {
                    if ('s' == b) {
                        state = 6;
                        continue block19;
                    }
                    state = 0;
                    continue block19;
                }
                case 6: {
                    if ('e' == b) {
                        state = 7;
                        continue block19;
                    }
                    state = 0;
                    continue block19;
                }
                case 7: {
                    if ('t' == b) {
                        state = 8;
                        continue block19;
                    }
                    state = 0;
                    continue block19;
                }
                case 8: {
                    if ('=' == b) {
                        state = 9;
                        continue block19;
                    }
                    if (' ' == b) continue block19;
                    state = 0;
                    continue block19;
                }
                case 9: {
                    if (' ' == b) continue block19;
                    builder = new StringBuilder();
                    builder.append(value, 0, start + 1);
                    state = 10;
                    continue block19;
                }
                case 10: {
                    if (';' != b) continue block19;
                    builder.append(b);
                    state = 11;
                    continue block19;
                }
                case 11: {
                    if (' ' == b) continue block19;
                    builder.append(b);
                }
            }
        }
        if (builder == null) {
            return value;
        }
        return builder.toString();
    }

    static {
        block67: {
            Properties props2;
            InputStreamReader reader;
            InputStream stream;
            String resourceName;
            block66: {
                LOG = Log.getLogger(MimeTypes.class);
                TYPES = new ArrayTrie<ByteBuffer>(512);
                __dftMimeMap = new HashMap<String, String>();
                __inferredEncodings = new HashMap<String, String>();
                __assumedEncodings = new HashMap<String, String>();
                CACHE = new ArrayTrie<Type>(512);
                for (Type type : Type.values()) {
                    CACHE.put(type.toString(), type);
                    TYPES.put(type.toString(), type.asBuffer());
                    int charset = type.toString().indexOf(";charset=");
                    if (charset > 0) {
                        String alt = type.toString().replace(";charset=", "; charset=");
                        CACHE.put(alt, type);
                        TYPES.put(alt, type.asBuffer());
                    }
                    if (!type.isCharsetAssumed()) continue;
                    __assumedEncodings.put(type.asString(), type.getCharsetString());
                }
                resourceName = "org/eclipse/jetty/http/mime.properties";
                try {
                    stream = MimeTypes.class.getClassLoader().getResourceAsStream(resourceName);
                    Throwable throwable = null;
                    try {
                        if (stream == null) {
                            LOG.warn("Missing mime-type resource: {}", resourceName);
                            break block66;
                        }
                        try {
                            reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                            Throwable throwable2 = null;
                            try {
                                props2 = new Properties();
                                props2.load(reader);
                                props2.stringPropertyNames().stream().filter(x -> x != null).forEach(x -> __dftMimeMap.put(StringUtil.asciiToLowerCase(x), MimeTypes.normalizeMimeType(props2.getProperty((String)x))));
                                if (__dftMimeMap.size() == 0) {
                                    LOG.warn("Empty mime types at {}", resourceName);
                                } else if (__dftMimeMap.size() < props2.keySet().size()) {
                                    LOG.warn("Duplicate or null mime-type extension in resource: {}", resourceName);
                                }
                            }
                            catch (Throwable props2) {
                                throwable2 = props2;
                                throw props2;
                            }
                            finally {
                                if (reader != null) {
                                    if (throwable2 != null) {
                                        try {
                                            reader.close();
                                        }
                                        catch (Throwable props2) {
                                            throwable2.addSuppressed(props2);
                                        }
                                    } else {
                                        reader.close();
                                    }
                                }
                            }
                        }
                        catch (IOException e) {
                            LOG.warn(e.toString(), new Object[0]);
                            LOG.debug(e);
                        }
                    }
                    catch (Throwable e) {
                        throwable = e;
                        throw e;
                    }
                    finally {
                        if (stream != null) {
                            if (throwable != null) {
                                try {
                                    stream.close();
                                }
                                catch (Throwable e) {
                                    throwable.addSuppressed(e);
                                }
                            } else {
                                stream.close();
                            }
                        }
                    }
                }
                catch (IOException e) {
                    LOG.warn(e.toString(), new Object[0]);
                    LOG.debug(e);
                }
            }
            resourceName = "org/eclipse/jetty/http/encoding.properties";
            try {
                stream = MimeTypes.class.getClassLoader().getResourceAsStream(resourceName);
                Throwable throwable = null;
                try {
                    if (stream == null) {
                        LOG.warn("Missing encoding resource: {}", resourceName);
                        break block67;
                    }
                    try {
                        reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                        Throwable throwable3 = null;
                        try {
                            props2 = new Properties();
                            props2.load(reader);
                            props2.stringPropertyNames().stream().filter(t -> t != null).forEach(t -> {
                                String charset = props2.getProperty((String)t);
                                if (charset.startsWith("-")) {
                                    __assumedEncodings.put((String)t, charset.substring(1));
                                } else {
                                    __inferredEncodings.put((String)t, props2.getProperty((String)t));
                                }
                            });
                            if (__inferredEncodings.size() == 0) {
                                LOG.warn("Empty encodings at {}", resourceName);
                            } else if (__inferredEncodings.size() + __assumedEncodings.size() < props2.keySet().size()) {
                                LOG.warn("Null or duplicate encodings in resource: {}", resourceName);
                            }
                        }
                        catch (Throwable throwable4) {
                            throwable3 = throwable4;
                            throw throwable4;
                        }
                        finally {
                            if (reader != null) {
                                if (throwable3 != null) {
                                    try {
                                        reader.close();
                                    }
                                    catch (Throwable throwable5) {
                                        throwable3.addSuppressed(throwable5);
                                    }
                                } else {
                                    reader.close();
                                }
                            }
                        }
                    }
                    catch (IOException e) {
                        LOG.warn(e.toString(), new Object[0]);
                        LOG.debug(e);
                    }
                }
                catch (Throwable throwable6) {
                    throwable = throwable6;
                    throw throwable6;
                }
                finally {
                    if (stream != null) {
                        if (throwable != null) {
                            try {
                                stream.close();
                            }
                            catch (Throwable throwable7) {
                                throwable.addSuppressed(throwable7);
                            }
                        } else {
                            stream.close();
                        }
                    }
                }
            }
            catch (IOException e) {
                LOG.warn(e.toString(), new Object[0]);
                LOG.debug(e);
            }
        }
    }

    public static enum Type {
        FORM_ENCODED("application/x-www-form-urlencoded"),
        MESSAGE_HTTP("message/http"),
        MULTIPART_BYTERANGES("multipart/byteranges"),
        TEXT_HTML("text/html"),
        TEXT_PLAIN("text/plain"),
        TEXT_XML("text/xml"),
        TEXT_JSON("text/json", StandardCharsets.UTF_8),
        APPLICATION_JSON("application/json", StandardCharsets.UTF_8),
        TEXT_HTML_8859_1("text/html;charset=iso-8859-1", TEXT_HTML),
        TEXT_HTML_UTF_8("text/html;charset=utf-8", TEXT_HTML),
        TEXT_PLAIN_8859_1("text/plain;charset=iso-8859-1", TEXT_PLAIN),
        TEXT_PLAIN_UTF_8("text/plain;charset=utf-8", TEXT_PLAIN),
        TEXT_XML_8859_1("text/xml;charset=iso-8859-1", TEXT_XML),
        TEXT_XML_UTF_8("text/xml;charset=utf-8", TEXT_XML),
        TEXT_JSON_8859_1("text/json;charset=iso-8859-1", TEXT_JSON),
        TEXT_JSON_UTF_8("text/json;charset=utf-8", TEXT_JSON),
        APPLICATION_JSON_8859_1("application/json;charset=iso-8859-1", APPLICATION_JSON),
        APPLICATION_JSON_UTF_8("application/json;charset=utf-8", APPLICATION_JSON);

        private final String _string;
        private final Type _base;
        private final ByteBuffer _buffer;
        private final Charset _charset;
        private final String _charsetString;
        private final boolean _assumedCharset;
        private final HttpField _field;

        private Type(String s) {
            this._string = s;
            this._buffer = BufferUtil.toBuffer(s);
            this._base = this;
            this._charset = null;
            this._charsetString = null;
            this._assumedCharset = false;
            this._field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, this._string);
        }

        private Type(String s, Type base) {
            this._string = s;
            this._buffer = BufferUtil.toBuffer(s);
            this._base = base;
            int i = s.indexOf(";charset=");
            this._charset = Charset.forName(s.substring(i + 9));
            this._charsetString = this._charset.toString().toLowerCase(Locale.ENGLISH);
            this._assumedCharset = false;
            this._field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, this._string);
        }

        private Type(String s, Charset cs) {
            this._string = s;
            this._base = this;
            this._buffer = BufferUtil.toBuffer(s);
            this._charset = cs;
            this._charsetString = this._charset == null ? null : this._charset.toString().toLowerCase(Locale.ENGLISH);
            this._assumedCharset = true;
            this._field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, this._string);
        }

        public ByteBuffer asBuffer() {
            return this._buffer.asReadOnlyBuffer();
        }

        public Charset getCharset() {
            return this._charset;
        }

        public String getCharsetString() {
            return this._charsetString;
        }

        public boolean is(String s) {
            return this._string.equalsIgnoreCase(s);
        }

        public String asString() {
            return this._string;
        }

        public String toString() {
            return this._string;
        }

        public boolean isCharsetAssumed() {
            return this._assumedCharset;
        }

        public HttpField getContentTypeField() {
            return this._field;
        }

        public Type getBaseType() {
            return this._base;
        }
    }
}

