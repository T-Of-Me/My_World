/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;

public interface HttpFieldPreEncoder {
    public HttpVersion getHttpVersion();

    public byte[] getEncodedField(HttpHeader var1, String var2, String var3);
}

