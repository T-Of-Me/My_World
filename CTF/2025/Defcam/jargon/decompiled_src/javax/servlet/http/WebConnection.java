/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

public interface WebConnection
extends AutoCloseable {
    public ServletInputStream getInputStream() throws IOException;

    public ServletOutputStream getOutputStream() throws IOException;
}

