/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.BitSet;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.QuotedStringTokenizer;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Credential;

public class DigestAuthenticator
extends LoginAuthenticator {
    private static final Logger LOG = Log.getLogger(DigestAuthenticator.class);
    private final SecureRandom _random = new SecureRandom();
    private long _maxNonceAgeMs = 60000L;
    private int _maxNC = 1024;
    private ConcurrentMap<String, Nonce> _nonceMap = new ConcurrentHashMap<String, Nonce>();
    private Queue<Nonce> _nonceQueue = new ConcurrentLinkedQueue<Nonce>();

    @Override
    public void setConfiguration(Authenticator.AuthConfiguration configuration) {
        String mnc;
        super.setConfiguration(configuration);
        String mna = configuration.getInitParameter("maxNonceAge");
        if (mna != null) {
            this.setMaxNonceAge(Long.valueOf(mna));
        }
        if ((mnc = configuration.getInitParameter("maxNonceCount")) != null) {
            this.setMaxNonceCount(Integer.valueOf(mnc));
        }
    }

    public int getMaxNonceCount() {
        return this._maxNC;
    }

    public void setMaxNonceCount(int maxNC) {
        this._maxNC = maxNC;
    }

    public long getMaxNonceAge() {
        return this._maxNonceAgeMs;
    }

    public void setMaxNonceAge(long maxNonceAgeInMillis) {
        this._maxNonceAgeMs = maxNonceAgeInMillis;
    }

    @Override
    public String getAuthMethod() {
        return "DIGEST";
    }

    @Override
    public boolean secureResponse(ServletRequest req, ServletResponse res, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException {
        if (!mandatory) {
            return new DeferredAuthentication(this);
        }
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        String credentials = request.getHeader(HttpHeader.AUTHORIZATION.asString());
        try {
            boolean stale = false;
            if (credentials != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Credentials: " + credentials, new Object[0]);
                }
                QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(credentials, "=, ", true, false);
                Digest digest = new Digest(request.getMethod());
                String last = null;
                String name = null;
                block7: while (tokenizer.hasMoreTokens()) {
                    String tok = tokenizer.nextToken();
                    char c = tok.length() == 1 ? tok.charAt(0) : (char)'\u0000';
                    switch (c) {
                        case '=': {
                            name = last;
                            last = tok;
                            continue block7;
                        }
                        case ',': {
                            name = null;
                            continue block7;
                        }
                        case ' ': {
                            continue block7;
                        }
                    }
                    last = tok;
                    if (name == null) continue;
                    if ("username".equalsIgnoreCase(name)) {
                        digest.username = tok;
                    } else if ("realm".equalsIgnoreCase(name)) {
                        digest.realm = tok;
                    } else if ("nonce".equalsIgnoreCase(name)) {
                        digest.nonce = tok;
                    } else if ("nc".equalsIgnoreCase(name)) {
                        digest.nc = tok;
                    } else if ("cnonce".equalsIgnoreCase(name)) {
                        digest.cnonce = tok;
                    } else if ("qop".equalsIgnoreCase(name)) {
                        digest.qop = tok;
                    } else if ("uri".equalsIgnoreCase(name)) {
                        digest.uri = tok;
                    } else if ("response".equalsIgnoreCase(name)) {
                        digest.response = tok;
                    }
                    name = null;
                }
                int n = this.checkNonce(digest, (Request)request);
                if (n > 0) {
                    UserIdentity user = this.login(digest.username, digest, req);
                    if (user != null) {
                        return new UserAuthentication(this.getAuthMethod(), user);
                    }
                } else if (n == 0) {
                    stale = true;
                }
            }
            if (!DeferredAuthentication.isDeferred(response)) {
                String domain = request.getContextPath();
                if (domain == null) {
                    domain = "/";
                }
                response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Digest realm=\"" + this._loginService.getName() + "\", domain=\"" + domain + "\", nonce=\"" + this.newNonce((Request)request) + "\", algorithm=MD5, qop=\"auth\", stale=" + stale);
                response.sendError(401);
                return Authentication.SEND_CONTINUE;
            }
            return Authentication.UNAUTHENTICATED;
        }
        catch (IOException e) {
            throw new ServerAuthException(e);
        }
    }

    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
        Digest digest = (Digest)credentials;
        if (!Objects.equals(digest.realm, this._loginService.getName())) {
            return null;
        }
        return super.login(username, credentials, request);
    }

    public String newNonce(Request request) {
        byte[] nounce;
        Nonce nonce;
        do {
            nounce = new byte[24];
            this._random.nextBytes(nounce);
        } while (this._nonceMap.putIfAbsent(nonce._nonce, nonce = new Nonce(new String(B64Code.encode(nounce)), request.getTimeStamp(), this.getMaxNonceCount())) != null);
        this._nonceQueue.add(nonce);
        return nonce._nonce;
    }

    private int checkNonce(Digest digest, Request request) {
        long expired = request.getTimeStamp() - this.getMaxNonceAge();
        Nonce nonce = this._nonceQueue.peek();
        while (nonce != null && nonce._ts < expired) {
            this._nonceQueue.remove(nonce);
            this._nonceMap.remove(nonce._nonce);
            nonce = this._nonceQueue.peek();
        }
        try {
            nonce = (Nonce)this._nonceMap.get(digest.nonce);
            if (nonce == null) {
                return 0;
            }
            long count = Long.parseLong(digest.nc, 16);
            if (count >= (long)this._maxNC) {
                return 0;
            }
            if (nonce.seen((int)count)) {
                return -1;
            }
            return 1;
        }
        catch (Exception e) {
            LOG.ignore(e);
            return -1;
        }
    }

    private static class Digest
    extends Credential {
        private static final long serialVersionUID = -2484639019549527724L;
        final String method;
        String username = "";
        String realm = "";
        String nonce = "";
        String nc = "";
        String cnonce = "";
        String qop = "";
        String uri = "";
        String response = "";

        Digest(String m) {
            this.method = m;
        }

        @Override
        public boolean check(Object credentials) {
            if (credentials instanceof char[]) {
                credentials = new String((char[])credentials);
            }
            String password = credentials instanceof String ? (String)credentials : credentials.toString();
            try {
                byte[] ha1;
                MessageDigest md = MessageDigest.getInstance("MD5");
                if (credentials instanceof Credential.MD5) {
                    ha1 = ((Credential.MD5)credentials).getDigest();
                } else {
                    md.update(this.username.getBytes(StandardCharsets.ISO_8859_1));
                    md.update((byte)58);
                    md.update(this.realm.getBytes(StandardCharsets.ISO_8859_1));
                    md.update((byte)58);
                    md.update(password.getBytes(StandardCharsets.ISO_8859_1));
                    ha1 = md.digest();
                }
                md.reset();
                md.update(this.method.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte)58);
                md.update(this.uri.getBytes(StandardCharsets.ISO_8859_1));
                byte[] ha2 = md.digest();
                md.update(TypeUtil.toString(ha1, 16).getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte)58);
                md.update(this.nonce.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte)58);
                md.update(this.nc.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte)58);
                md.update(this.cnonce.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte)58);
                md.update(this.qop.getBytes(StandardCharsets.ISO_8859_1));
                md.update((byte)58);
                md.update(TypeUtil.toString(ha2, 16).getBytes(StandardCharsets.ISO_8859_1));
                byte[] digest = md.digest();
                return Digest.stringEquals(TypeUtil.toString(digest, 16).toLowerCase(), this.response == null ? null : this.response.toLowerCase());
            }
            catch (Exception e) {
                LOG.warn(e);
                return false;
            }
        }

        public String toString() {
            return this.username + "," + this.response;
        }
    }

    private static class Nonce {
        final String _nonce;
        final long _ts;
        final BitSet _seen;

        public Nonce(String nonce, long ts, int size) {
            this._nonce = nonce;
            this._ts = ts;
            this._seen = new BitSet(size);
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public boolean seen(int count) {
            Nonce nonce = this;
            synchronized (nonce) {
                if (count >= this._seen.size()) {
                    return true;
                }
                boolean s = this._seen.get(count);
                this._seen.set(count);
                return s;
            }
        }
    }
}

