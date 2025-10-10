/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.PreEncodedHttpField;

public class CompressedContentFormat {
    public static final CompressedContentFormat GZIP = new CompressedContentFormat("gzip", ".gz");
    public static final CompressedContentFormat BR = new CompressedContentFormat("br", ".br");
    public static final CompressedContentFormat[] NONE = new CompressedContentFormat[0];
    public final String _encoding;
    public final String _extension;
    public final String _etag;
    public final String _etagQuote;
    public final PreEncodedHttpField _contentEncoding;

    public CompressedContentFormat(String encoding, String extension) {
        this._encoding = encoding;
        this._extension = extension;
        this._etag = "--" + encoding;
        this._etagQuote = this._etag + "\"";
        this._contentEncoding = new PreEncodedHttpField(HttpHeader.CONTENT_ENCODING, encoding);
    }

    public boolean equals(Object o) {
        if (!(o instanceof CompressedContentFormat)) {
            return false;
        }
        CompressedContentFormat ccf = (CompressedContentFormat)o;
        if (this._encoding == null && ccf._encoding != null) {
            return false;
        }
        if (this._extension == null && ccf._extension != null) {
            return false;
        }
        return this._encoding.equalsIgnoreCase(ccf._encoding) && this._extension.equalsIgnoreCase(ccf._extension);
    }

    public static boolean tagEquals(String etag, String tag) {
        if (etag.equals(tag)) {
            return true;
        }
        int dashdash = tag.indexOf("--");
        if (dashdash > 0) {
            return etag.regionMatches(0, tag, 0, dashdash - 2);
        }
        return false;
    }
}

