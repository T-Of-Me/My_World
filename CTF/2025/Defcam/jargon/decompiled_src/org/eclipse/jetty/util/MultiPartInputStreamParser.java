/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Part;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.ReadLineInputStream;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class MultiPartInputStreamParser {
    private static final Logger LOG = Log.getLogger(MultiPartInputStreamParser.class);
    public static final MultipartConfigElement __DEFAULT_MULTIPART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
    public static final MultiMap<Part> EMPTY_MAP = new MultiMap(Collections.emptyMap());
    protected InputStream _in;
    protected MultipartConfigElement _config;
    protected String _contentType;
    protected MultiMap<Part> _parts;
    protected Exception _err;
    protected File _tmpDir;
    protected File _contextTmpDir;
    protected boolean _deleteOnExit;
    protected boolean _writeFilesWithFilenames;

    public MultiPartInputStreamParser(InputStream in, String contentType, MultipartConfigElement config, File contextTmpDir) {
        this._contentType = contentType;
        this._config = config;
        this._contextTmpDir = contextTmpDir;
        if (this._contextTmpDir == null) {
            this._contextTmpDir = new File(System.getProperty("java.io.tmpdir"));
        }
        if (this._config == null) {
            this._config = new MultipartConfigElement(this._contextTmpDir.getAbsolutePath());
        }
        if (in instanceof ServletInputStream && ((ServletInputStream)in).isFinished()) {
            this._parts = EMPTY_MAP;
            return;
        }
        this._in = new ReadLineInputStream(in);
    }

    public Collection<Part> getParsedParts() {
        if (this._parts == null) {
            return Collections.emptyList();
        }
        Collection values = this._parts.values();
        ArrayList<Part> parts = new ArrayList<Part>();
        for (List o : values) {
            List asList = LazyList.getList(o, false);
            parts.addAll(asList);
        }
        return parts;
    }

    public void deleteParts() throws MultiException {
        Collection<Part> parts = this.getParsedParts();
        MultiException err = new MultiException();
        for (Part p : parts) {
            try {
                ((MultiPart)p).cleanUp();
            }
            catch (Exception e) {
                err.add(e);
            }
        }
        this._parts.clear();
        err.ifExceptionThrowMulti();
    }

    public Collection<Part> getParts() throws IOException {
        this.parse();
        this.throwIfError();
        Collection values = this._parts.values();
        ArrayList<Part> parts = new ArrayList<Part>();
        for (List o : values) {
            List asList = LazyList.getList(o, false);
            parts.addAll(asList);
        }
        return parts;
    }

    public Part getPart(String name) throws IOException {
        this.parse();
        this.throwIfError();
        return this._parts.getValue(name, 0);
    }

    protected void throwIfError() throws IOException {
        if (this._err != null) {
            if (this._err instanceof IOException) {
                throw (IOException)this._err;
            }
            if (this._err instanceof IllegalStateException) {
                throw (IllegalStateException)this._err;
            }
            throw new IllegalStateException(this._err);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void parse() {
        block44: {
            if (this._parts != null || this._err != null) {
                return;
            }
            long total = 0L;
            this._parts = new MultiMap();
            if (this._contentType == null || !this._contentType.startsWith("multipart/form-data")) {
                return;
            }
            try {
                File f;
                this._tmpDir = this._config.getLocation() == null ? this._contextTmpDir : ("".equals(this._config.getLocation()) ? this._contextTmpDir : ((f = new File(this._config.getLocation())).isAbsolute() ? f : new File(this._contextTmpDir, this._config.getLocation())));
                if (!this._tmpDir.exists()) {
                    this._tmpDir.mkdirs();
                }
                String contentTypeBoundary = "";
                int bstart = this._contentType.indexOf("boundary=");
                if (bstart >= 0) {
                    int bend = this._contentType.indexOf(";", bstart);
                    bend = bend < 0 ? this._contentType.length() : bend;
                    contentTypeBoundary = QuotedStringTokenizer.unquote(this.value(this._contentType.substring(bstart, bend)).trim());
                }
                String boundary = "--" + contentTypeBoundary;
                String lastBoundary = boundary + "--";
                byte[] byteBoundary = lastBoundary.getBytes(StandardCharsets.ISO_8859_1);
                String line = null;
                try {
                    line = ((ReadLineInputStream)this._in).readLine();
                }
                catch (IOException e) {
                    LOG.warn("Badly formatted multipart request", new Object[0]);
                    throw e;
                }
                if (line == null) {
                    throw new IOException("Missing content for multipart request");
                }
                boolean badFormatLogged = false;
                line = line.trim();
                while (line != null && !line.equals(boundary) && !line.equals(lastBoundary)) {
                    if (!badFormatLogged) {
                        LOG.warn("Badly formatted multipart request", new Object[0]);
                        badFormatLogged = true;
                    }
                    line = (line = ((ReadLineInputStream)this._in).readLine()) == null ? line : line.trim();
                }
                if (line == null || line.length() == 0) {
                    throw new IOException("Missing initial multi part boundary");
                }
                if (line.equals(lastBoundary)) {
                    return;
                }
                boolean lastPart = false;
                block8: while (!lastPart) {
                    String contentDisposition = null;
                    String contentType = null;
                    String contentTransferEncoding = null;
                    MultiMap<String> headers = new MultiMap<String>();
                    while ((line = ((ReadLineInputStream)this._in).readLine()) != null) {
                        if (!"".equals(line)) {
                            if (this._config.getMaxRequestSize() > 0L && (total += (long)line.length()) > this._config.getMaxRequestSize()) {
                                throw new IllegalStateException("Request exceeds maxRequestSize (" + this._config.getMaxRequestSize() + ")");
                            }
                            int c = line.indexOf(58, 0);
                            if (c <= 0) continue;
                            String key = line.substring(0, c).trim().toLowerCase(Locale.ENGLISH);
                            String value = line.substring(c + 1, line.length()).trim();
                            headers.put(key, value);
                            if (key.equalsIgnoreCase("content-disposition")) {
                                contentDisposition = value;
                            }
                            if (key.equalsIgnoreCase("content-type")) {
                                contentType = value;
                            }
                            if (!key.equals("content-transfer-encoding")) continue;
                            contentTransferEncoding = value;
                            continue;
                        }
                        boolean form_data = false;
                        if (contentDisposition == null) {
                            throw new IOException("Missing content-disposition");
                        }
                        QuotedStringTokenizer tok = new QuotedStringTokenizer(contentDisposition, ";", false, true);
                        String name = null;
                        String filename = null;
                        while (tok.hasMoreTokens()) {
                            String t = tok.nextToken().trim();
                            String tl = t.toLowerCase(Locale.ENGLISH);
                            if (t.startsWith("form-data")) {
                                form_data = true;
                                continue;
                            }
                            if (tl.startsWith("name=")) {
                                name = this.value(t);
                                continue;
                            }
                            if (!tl.startsWith("filename=")) continue;
                            filename = this.filenameValue(t);
                        }
                        if (!form_data || name == null) continue block8;
                        MultiPart part = new MultiPart(name, filename);
                        part.setHeaders(headers);
                        part.setContentType(contentType);
                        this._parts.add(name, part);
                        part.open();
                        InputStream partInput = null;
                        partInput = "base64".equalsIgnoreCase(contentTransferEncoding) ? new Base64InputStream((ReadLineInputStream)this._in) : ("quoted-printable".equalsIgnoreCase(contentTransferEncoding) ? new FilterInputStream(this._in){

                            @Override
                            public int read() throws IOException {
                                int c = this.in.read();
                                if (c >= 0 && c == 61) {
                                    int hi = this.in.read();
                                    int lo = this.in.read();
                                    if (hi < 0 || lo < 0) {
                                        throw new IOException("Unexpected end to quoted-printable byte");
                                    }
                                    char[] chars = new char[]{(char)hi, (char)lo};
                                    c = Integer.parseInt(new String(chars), 16);
                                }
                                return c;
                            }
                        } : this._in);
                        try {
                            int state = -2;
                            boolean cr = false;
                            boolean lf = false;
                            while (true) {
                                int c;
                                int b = 0;
                                while ((c = state != -2 ? state : partInput.read()) != -1) {
                                    if (this._config.getMaxRequestSize() > 0L && ++total > this._config.getMaxRequestSize()) {
                                        throw new IllegalStateException("Request exceeds maxRequestSize (" + this._config.getMaxRequestSize() + ")");
                                    }
                                    state = -2;
                                    if (c == 13 || c == 10) {
                                        if (c != 13) break;
                                        partInput.mark(1);
                                        int tmp = partInput.read();
                                        if (tmp != 10) {
                                            partInput.reset();
                                            break;
                                        }
                                        state = tmp;
                                        break;
                                    }
                                    if (b >= 0 && b < byteBoundary.length && c == byteBoundary[b]) {
                                        ++b;
                                        continue;
                                    }
                                    if (cr) {
                                        part.write(13);
                                    }
                                    if (lf) {
                                        part.write(10);
                                    }
                                    lf = false;
                                    cr = false;
                                    if (b > 0) {
                                        part.write(byteBoundary, 0, b);
                                    }
                                    b = -1;
                                    part.write(c);
                                }
                                if (b > 0 && b < byteBoundary.length - 2 || b == byteBoundary.length - 1) {
                                    if (cr) {
                                        part.write(13);
                                    }
                                    if (lf) {
                                        part.write(10);
                                    }
                                    lf = false;
                                    cr = false;
                                    part.write(byteBoundary, 0, b);
                                    b = -1;
                                }
                                if (b > 0 || c == -1) {
                                    if (b == byteBoundary.length) {
                                        lastPart = true;
                                    }
                                    if (state != 10) continue block8;
                                    state = -2;
                                    continue block8;
                                }
                                if (cr) {
                                    part.write(13);
                                }
                                if (lf) {
                                    part.write(10);
                                }
                                cr = c == 13;
                                boolean bl = lf = c == 10 || state == 10;
                                if (state != 10) continue;
                                state = -2;
                            }
                        }
                        finally {
                            part.close();
                            continue block8;
                        }
                    }
                    break block8;
                }
                if (lastPart) {
                    while (line != null) {
                        line = ((ReadLineInputStream)this._in).readLine();
                    }
                    break block44;
                }
                throw new IOException("Incomplete parts");
            }
            catch (Exception e) {
                this._err = e;
            }
        }
    }

    public void setDeleteOnExit(boolean deleteOnExit) {
        this._deleteOnExit = deleteOnExit;
    }

    public void setWriteFilesWithFilenames(boolean writeFilesWithFilenames) {
        this._writeFilesWithFilenames = writeFilesWithFilenames;
    }

    public boolean isWriteFilesWithFilenames() {
        return this._writeFilesWithFilenames;
    }

    public boolean isDeleteOnExit() {
        return this._deleteOnExit;
    }

    private String value(String nameEqualsValue) {
        int idx = nameEqualsValue.indexOf(61);
        String value = nameEqualsValue.substring(idx + 1).trim();
        return QuotedStringTokenizer.unquoteOnly(value);
    }

    private String filenameValue(String nameEqualsValue) {
        int idx = nameEqualsValue.indexOf(61);
        String value = nameEqualsValue.substring(idx + 1).trim();
        if (value.matches(".??[a-z,A-Z]\\:\\\\[^\\\\].*")) {
            char last;
            char first = value.charAt(0);
            if (first == '\"' || first == '\'') {
                value = value.substring(1);
            }
            if ((last = value.charAt(value.length() - 1)) == '\"' || last == '\'') {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        return QuotedStringTokenizer.unquoteOnly(value, true);
    }

    private static class Base64InputStream
    extends InputStream {
        ReadLineInputStream _in;
        String _line;
        byte[] _buffer;
        int _pos;

        public Base64InputStream(ReadLineInputStream rlis) {
            this._in = rlis;
        }

        @Override
        public int read() throws IOException {
            if (this._buffer == null || this._pos >= this._buffer.length) {
                this._line = this._in.readLine();
                if (this._line == null) {
                    return -1;
                }
                if (this._line.startsWith("--")) {
                    this._buffer = (this._line + "\r\n").getBytes();
                } else if (this._line.length() == 0) {
                    this._buffer = "\r\n".getBytes();
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * this._line.length() / 3 + 2);
                    B64Code.decode(this._line, baos);
                    baos.write(13);
                    baos.write(10);
                    this._buffer = baos.toByteArray();
                }
                this._pos = 0;
            }
            return this._buffer[this._pos++];
        }
    }

    public class MultiPart
    implements Part {
        protected String _name;
        protected String _filename;
        protected File _file;
        protected OutputStream _out;
        protected ByteArrayOutputStream2 _bout;
        protected String _contentType;
        protected MultiMap<String> _headers;
        protected long _size = 0L;
        protected boolean _temporary = true;

        public MultiPart(String name, String filename) throws IOException {
            this._name = name;
            this._filename = filename;
        }

        public String toString() {
            return String.format("Part{n=%s,fn=%s,ct=%s,s=%d,t=%b,f=%s}", this._name, this._filename, this._contentType, this._size, this._temporary, this._file);
        }

        protected void setContentType(String contentType) {
            this._contentType = contentType;
        }

        protected void open() throws IOException {
            if (MultiPartInputStreamParser.this.isWriteFilesWithFilenames() && this._filename != null && this._filename.trim().length() > 0) {
                this.createFile();
            } else {
                this._bout = new ByteArrayOutputStream2();
                this._out = this._bout;
            }
        }

        protected void close() throws IOException {
            this._out.close();
        }

        protected void write(int b) throws IOException {
            if (MultiPartInputStreamParser.this._config.getMaxFileSize() > 0L && this._size + 1L > MultiPartInputStreamParser.this._config.getMaxFileSize()) {
                throw new IllegalStateException("Multipart Mime part " + this._name + " exceeds max filesize");
            }
            if (MultiPartInputStreamParser.this._config.getFileSizeThreshold() > 0 && this._size + 1L > (long)MultiPartInputStreamParser.this._config.getFileSizeThreshold() && this._file == null) {
                this.createFile();
            }
            this._out.write(b);
            ++this._size;
        }

        protected void write(byte[] bytes, int offset, int length) throws IOException {
            if (MultiPartInputStreamParser.this._config.getMaxFileSize() > 0L && this._size + (long)length > MultiPartInputStreamParser.this._config.getMaxFileSize()) {
                throw new IllegalStateException("Multipart Mime part " + this._name + " exceeds max filesize");
            }
            if (MultiPartInputStreamParser.this._config.getFileSizeThreshold() > 0 && this._size + (long)length > (long)MultiPartInputStreamParser.this._config.getFileSizeThreshold() && this._file == null) {
                this.createFile();
            }
            this._out.write(bytes, offset, length);
            this._size += (long)length;
        }

        protected void createFile() throws IOException {
            boolean USER = true;
            boolean WORLD = false;
            this._file = File.createTempFile("MultiPart", "", MultiPartInputStreamParser.this._tmpDir);
            this._file.setReadable(false, false);
            this._file.setReadable(true, true);
            if (MultiPartInputStreamParser.this._deleteOnExit) {
                this._file.deleteOnExit();
            }
            FileOutputStream fos = new FileOutputStream(this._file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            if (this._size > 0L && this._out != null) {
                this._out.flush();
                this._bout.writeTo(bos);
                this._out.close();
                this._bout = null;
            }
            this._out = bos;
        }

        protected void setHeaders(MultiMap<String> headers) {
            this._headers = headers;
        }

        @Override
        public String getContentType() {
            return this._contentType;
        }

        @Override
        public String getHeader(String name) {
            if (name == null) {
                return null;
            }
            return this._headers.getValue(name.toLowerCase(Locale.ENGLISH), 0);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return this._headers.keySet();
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return this._headers.getValues(name);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (this._file != null) {
                return new BufferedInputStream(new FileInputStream(this._file));
            }
            return new ByteArrayInputStream(this._bout.getBuf(), 0, this._bout.size());
        }

        @Override
        public String getSubmittedFileName() {
            return this.getContentDispositionFilename();
        }

        public byte[] getBytes() {
            if (this._bout != null) {
                return this._bout.toByteArray();
            }
            return null;
        }

        @Override
        public String getName() {
            return this._name;
        }

        @Override
        public long getSize() {
            return this._size;
        }

        @Override
        public void write(String fileName) throws IOException {
            if (this._file == null) {
                this._temporary = false;
                this._file = new File(MultiPartInputStreamParser.this._tmpDir, fileName);
                FilterOutputStream bos = null;
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(this._file));
                    this._bout.writeTo(bos);
                    ((BufferedOutputStream)bos).flush();
                }
                finally {
                    if (bos != null) {
                        bos.close();
                    }
                    this._bout = null;
                }
            } else {
                this._temporary = false;
                Path src = this._file.toPath();
                Path target = src.resolveSibling(fileName);
                Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
                this._file = target.toFile();
            }
        }

        @Override
        public void delete() throws IOException {
            if (this._file != null && this._file.exists()) {
                this._file.delete();
            }
        }

        public void cleanUp() throws IOException {
            if (this._temporary && this._file != null && this._file.exists()) {
                this._file.delete();
            }
        }

        public File getFile() {
            return this._file;
        }

        public String getContentDispositionFilename() {
            return this._filename;
        }
    }
}

