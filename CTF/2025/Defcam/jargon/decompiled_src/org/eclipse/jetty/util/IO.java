/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.charset.Charset;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class IO {
    private static final Logger LOG = Log.getLogger(IO.class);
    public static final String CRLF = "\r\n";
    public static final byte[] CRLF_BYTES = new byte[]{13, 10};
    public static final int bufferSize = 65536;
    private static NullOS __nullStream = new NullOS();
    private static ClosedIS __closedStream = new ClosedIS();
    private static NullWrite __nullWriter = new NullWrite();
    private static PrintWriter __nullPrintWriter = new PrintWriter(__nullWriter);

    public static void copy(InputStream in, OutputStream out) throws IOException {
        IO.copy(in, out, -1L);
    }

    public static void copy(Reader in, Writer out) throws IOException {
        IO.copy(in, out, -1L);
    }

    public static void copy(InputStream in, OutputStream out, long byteCount) throws IOException {
        byte[] buffer = new byte[65536];
        int len = 65536;
        if (byteCount >= 0L) {
            int max;
            while (byteCount > 0L && (len = in.read(buffer, 0, max = byteCount < 65536L ? (int)byteCount : 65536)) != -1) {
                byteCount -= (long)len;
                out.write(buffer, 0, len);
            }
        } else {
            while ((len = in.read(buffer, 0, 65536)) >= 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    public static void copy(Reader in, Writer out, long byteCount) throws IOException {
        char[] buffer = new char[65536];
        int len = 65536;
        if (byteCount >= 0L) {
            while (byteCount > 0L && (len = byteCount < 65536L ? in.read(buffer, 0, (int)byteCount) : in.read(buffer, 0, 65536)) != -1) {
                byteCount -= (long)len;
                out.write(buffer, 0, len);
            }
        } else if (out instanceof PrintWriter) {
            PrintWriter pout = (PrintWriter)out;
            while (!pout.checkError() && (len = in.read(buffer, 0, 65536)) != -1) {
                out.write(buffer, 0, len);
            }
        } else {
            while ((len = in.read(buffer, 0, 65536)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    public static void copy(File from, File to) throws IOException {
        if (from.isDirectory()) {
            IO.copyDir(from, to);
        } else {
            IO.copyFile(from, to);
        }
    }

    public static void copyDir(File from, File to) throws IOException {
        File[] files;
        if (to.exists()) {
            if (!to.isDirectory()) {
                throw new IllegalArgumentException(to.toString());
            }
        } else {
            to.mkdirs();
        }
        if ((files = from.listFiles()) != null) {
            for (int i = 0; i < files.length; ++i) {
                String name = files[i].getName();
                if (".".equals(name) || "..".equals(name)) continue;
                IO.copy(files[i], new File(to, name));
            }
        }
    }

    public static void copyFile(File from, File to) throws IOException {
        try (FileInputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to);){
            IO.copy(in, out);
        }
    }

    public static String toString(InputStream in) throws IOException {
        return IO.toString(in, (Charset)null);
    }

    public static String toString(InputStream in, String encoding) throws IOException {
        return IO.toString(in, encoding == null ? null : Charset.forName(encoding));
    }

    public static String toString(InputStream in, Charset encoding) throws IOException {
        StringWriter writer = new StringWriter();
        InputStreamReader reader = encoding == null ? new InputStreamReader(in) : new InputStreamReader(in, encoding);
        IO.copy(reader, writer);
        return writer.toString();
    }

    public static String toString(Reader in) throws IOException {
        StringWriter writer = new StringWriter();
        IO.copy(in, writer);
        return writer.toString();
    }

    public static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; files != null && i < files.length; ++i) {
                IO.delete(files[i]);
            }
        }
        return file.delete();
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        }
        catch (IOException ignore) {
            LOG.ignore(ignore);
        }
    }

    public static void close(InputStream is) {
        IO.close((Closeable)is);
    }

    public static void close(OutputStream os) {
        IO.close((Closeable)os);
    }

    public static void close(Reader reader) {
        IO.close((Closeable)reader);
    }

    public static void close(Writer writer) {
        IO.close((Closeable)writer);
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IO.copy(in, bout);
        return bout.toByteArray();
    }

    public static long write(GatheringByteChannel out, ByteBuffer[] buffers, int offset, int length) throws IOException {
        long wrote;
        long total = 0L;
        block0: while (length > 0 && (wrote = out.write(buffers, offset, length)) != 0L) {
            total += wrote;
            for (int i = offset; i < buffers.length; ++i) {
                if (!buffers[i].hasRemaining()) continue;
                length -= i - offset;
                offset = i;
                continue block0;
            }
            length = 0;
        }
        return total;
    }

    public static OutputStream getNullStream() {
        return __nullStream;
    }

    public static InputStream getClosedStream() {
        return __closedStream;
    }

    public static Writer getNullWriter() {
        return __nullWriter;
    }

    public static PrintWriter getNullPrintWriter() {
        return __nullPrintWriter;
    }

    private static class NullWrite
    extends Writer {
        private NullWrite() {
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(char[] b) {
        }

        @Override
        public void write(char[] b, int o, int l) {
        }

        @Override
        public void write(int b) {
        }

        @Override
        public void write(String s) {
        }

        @Override
        public void write(String s, int o, int l) {
        }
    }

    private static class ClosedIS
    extends InputStream {
        private ClosedIS() {
        }

        @Override
        public int read() throws IOException {
            return -1;
        }
    }

    private static class NullOS
    extends OutputStream {
        private NullOS() {
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(byte[] b) {
        }

        @Override
        public void write(byte[] b, int i, int l) {
        }

        @Override
        public void write(int b) {
        }
    }

    static class Job
    implements Runnable {
        InputStream in;
        OutputStream out;
        Reader read;
        Writer write;

        Job(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            this.read = null;
            this.write = null;
        }

        Job(Reader read, Writer write) {
            this.in = null;
            this.out = null;
            this.read = read;
            this.write = write;
        }

        @Override
        public void run() {
            try {
                if (this.in != null) {
                    IO.copy(this.in, this.out, -1L);
                } else {
                    IO.copy(this.read, this.write, -1L);
                }
            }
            catch (IOException e) {
                LOG.ignore(e);
                try {
                    if (this.out != null) {
                        this.out.close();
                    }
                    if (this.write != null) {
                        this.write.close();
                    }
                }
                catch (IOException e2) {
                    LOG.ignore(e2);
                }
            }
        }
    }
}

