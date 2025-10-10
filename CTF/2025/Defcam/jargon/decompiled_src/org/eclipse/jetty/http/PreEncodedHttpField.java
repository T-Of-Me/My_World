/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.eclipse.jetty.http.Http1FieldPreEncoder;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFieldPreEncoder;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class PreEncodedHttpField
extends HttpField {
    private static final Logger LOG = Log.getLogger(PreEncodedHttpField.class);
    private static final HttpFieldPreEncoder[] __encoders;
    private final byte[][] _encodedField = new byte[__encoders.length][];

    private static int index(HttpVersion version) {
        switch (version) {
            case HTTP_1_0: 
            case HTTP_1_1: {
                return 0;
            }
            case HTTP_2: {
                return 1;
            }
        }
        return -1;
    }

    public PreEncodedHttpField(HttpHeader header, String name, String value) {
        super(header, name, value);
        for (int i = 0; i < __encoders.length; ++i) {
            this._encodedField[i] = __encoders[i].getEncodedField(header, header.asString(), value);
        }
    }

    public PreEncodedHttpField(HttpHeader header, String value) {
        this(header, header.asString(), value);
    }

    public PreEncodedHttpField(String name, String value) {
        this(null, name, value);
    }

    public void putTo(ByteBuffer bufferInFillMode, HttpVersion version) {
        bufferInFillMode.put(this._encodedField[PreEncodedHttpField.index(version)]);
    }

    static {
        ArrayList<HttpFieldPreEncoder> encoders = new ArrayList<HttpFieldPreEncoder>();
        Iterator<HttpFieldPreEncoder> iter = ServiceLoader.load(HttpFieldPreEncoder.class).iterator();
        while (iter.hasNext()) {
            try {
                HttpFieldPreEncoder encoder = iter.next();
                if (PreEncodedHttpField.index(encoder.getHttpVersion()) < 0) continue;
                encoders.add(encoder);
            }
            catch (Error | RuntimeException e) {
                LOG.debug(e);
            }
        }
        LOG.debug("HttpField encoders loaded: {}", encoders);
        int size = encoders.size();
        __encoders = new HttpFieldPreEncoder[size == 0 ? 1 : size];
        for (HttpFieldPreEncoder e : encoders) {
            int i = PreEncodedHttpField.index(e.getHttpVersion());
            if (__encoders[i] == null) {
                PreEncodedHttpField.__encoders[i] = e;
                continue;
            }
            LOG.warn("multiple PreEncoders for " + (Object)((Object)e.getHttpVersion()), new Object[0]);
        }
        if (__encoders[0] == null) {
            PreEncodedHttpField.__encoders[0] = new Http1FieldPreEncoder();
        }
    }
}

