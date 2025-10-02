/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import javax.servlet.annotation.MultipartConfig;

public class MultipartConfigElement {
    private String location;
    private long maxFileSize;
    private long maxRequestSize;
    private int fileSizeThreshold;

    public MultipartConfigElement(String location) {
        this.location = location == null ? "" : location;
        this.maxFileSize = -1L;
        this.maxRequestSize = -1L;
        this.fileSizeThreshold = 0;
    }

    public MultipartConfigElement(String location, long maxFileSize, long maxRequestSize, int fileSizeThreshold) {
        this.location = location == null ? "" : location;
        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
        this.fileSizeThreshold = fileSizeThreshold;
    }

    public MultipartConfigElement(MultipartConfig annotation) {
        this.location = annotation.location();
        this.fileSizeThreshold = annotation.fileSizeThreshold();
        this.maxFileSize = annotation.maxFileSize();
        this.maxRequestSize = annotation.maxRequestSize();
    }

    public String getLocation() {
        return this.location;
    }

    public long getMaxFileSize() {
        return this.maxFileSize;
    }

    public long getMaxRequestSize() {
        return this.maxRequestSize;
    }

    public int getFileSizeThreshold() {
        return this.fileSizeThreshold;
    }
}

