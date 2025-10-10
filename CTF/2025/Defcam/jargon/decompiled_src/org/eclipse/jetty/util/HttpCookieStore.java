/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class HttpCookieStore
implements CookieStore {
    private final CookieStore delegate = new CookieManager().getCookieStore();

    @Override
    public void add(URI uri, HttpCookie cookie) {
        this.delegate.add(uri, cookie);
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return this.delegate.get(uri);
    }

    @Override
    public List<HttpCookie> getCookies() {
        return this.delegate.getCookies();
    }

    @Override
    public List<URI> getURIs() {
        return this.delegate.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        return this.delegate.remove(uri, cookie);
    }

    @Override
    public boolean removeAll() {
        return this.delegate.removeAll();
    }

    public static class Empty
    implements CookieStore {
        @Override
        public void add(URI uri, HttpCookie cookie) {
        }

        @Override
        public List<HttpCookie> get(URI uri) {
            return Collections.emptyList();
        }

        @Override
        public List<HttpCookie> getCookies() {
            return Collections.emptyList();
        }

        @Override
        public List<URI> getURIs() {
            return Collections.emptyList();
        }

        @Override
        public boolean remove(URI uri, HttpCookie cookie) {
            return false;
        }

        @Override
        public boolean removeAll() {
            return false;
        }
    }
}

