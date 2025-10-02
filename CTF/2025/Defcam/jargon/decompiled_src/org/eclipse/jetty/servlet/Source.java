/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet;

public class Source {
    public static final Source EMBEDDED = new Source(Origin.EMBEDDED, null);
    public static final Source JAVAX_API = new Source(Origin.JAVAX_API, null);
    public Origin _origin;
    public String _resource;

    public Source(Origin o, String resource) {
        if (o == null) {
            throw new IllegalArgumentException("Origin is null");
        }
        this._origin = o;
        this._resource = resource;
    }

    public Origin getOrigin() {
        return this._origin;
    }

    public String getResource() {
        return this._resource;
    }

    public String toString() {
        return (Object)((Object)this._origin) + ":" + this._resource;
    }

    public static enum Origin {
        EMBEDDED,
        JAVAX_API,
        DESCRIPTOR,
        ANNOTATION;

    }
}

