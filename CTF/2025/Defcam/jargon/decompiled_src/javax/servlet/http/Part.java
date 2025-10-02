/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface Part {
    public InputStream getInputStream() throws IOException;

    public String getContentType();

    public String getName();

    public String getSubmittedFileName();

    public long getSize();

    public void write(String var1) throws IOException;

    public void delete() throws IOException;

    public String getHeader(String var1);

    public Collection<String> getHeaders(String var1);

    public Collection<String> getHeaderNames();
}

