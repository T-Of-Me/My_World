/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.security;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ServiceLoader;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.CredentialProvider;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.security.UnixCrypt;

public abstract class Credential
implements Serializable {
    private static final long serialVersionUID = -7760551052768181572L;
    private static final Logger LOG = Log.getLogger(Credential.class);
    private static final ServiceLoader<CredentialProvider> CREDENTIAL_PROVIDER_LOADER = ServiceLoader.load(CredentialProvider.class);

    public abstract boolean check(Object var1);

    public static Credential getCredential(String credential) {
        if (credential.startsWith("CRYPT:")) {
            return new Crypt(credential);
        }
        if (credential.startsWith("MD5:")) {
            return new MD5(credential);
        }
        for (CredentialProvider cp : CREDENTIAL_PROVIDER_LOADER) {
            Credential credentialObj;
            if (!credential.startsWith(cp.getPrefix()) || (credentialObj = cp.getCredential(credential)) == null) continue;
            return credentialObj;
        }
        return new Password(credential);
    }

    protected static boolean stringEquals(String s1, String s2) {
        int l2;
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        boolean result = true;
        int l1 = s1.length();
        if (l1 != (l2 = s2.length())) {
            result = false;
        }
        int l = Math.min(l1, l2);
        for (int i = 0; i < l; ++i) {
            result &= s1.charAt(i) == s2.charAt(i);
        }
        return result;
    }

    protected static boolean byteEquals(byte[] b1, byte[] b2) {
        if (b1 == b2) {
            return true;
        }
        if (b1 == null || b2 == null) {
            return false;
        }
        boolean result = true;
        int l1 = b1.length;
        int l2 = b2.length;
        if (l1 != l2) {
            result = false;
        }
        int l = Math.min(l1, l2);
        for (int i = 0; i < l; ++i) {
            result &= b1[i] == b2[i];
        }
        return result;
    }

    public static class MD5
    extends Credential {
        private static final long serialVersionUID = 5533846540822684240L;
        private static final String __TYPE = "MD5:";
        private static final Object __md5Lock = new Object();
        private static MessageDigest __md;
        private final byte[] _digest;

        MD5(String digest) {
            digest = digest.startsWith(__TYPE) ? digest.substring(__TYPE.length()) : digest;
            this._digest = TypeUtil.parseBytes(digest, 16);
        }

        public byte[] getDigest() {
            return this._digest;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public boolean check(Object credentials) {
            try {
                if (credentials instanceof char[]) {
                    credentials = new String((char[])credentials);
                }
                if (credentials instanceof Password || credentials instanceof String) {
                    byte[] digest;
                    Object object = __md5Lock;
                    synchronized (object) {
                        if (__md == null) {
                            __md = MessageDigest.getInstance("MD5");
                        }
                        __md.reset();
                        __md.update(credentials.toString().getBytes(StandardCharsets.ISO_8859_1));
                        digest = __md.digest();
                    }
                    return MD5.byteEquals(this._digest, digest);
                }
                if (credentials instanceof MD5) {
                    return this.equals(credentials);
                }
                if (credentials instanceof Credential) {
                    return ((Credential)credentials).check(this);
                }
                LOG.warn("Can't check " + credentials.getClass() + " against MD5", new Object[0]);
                return false;
            }
            catch (Exception e) {
                LOG.warn(e);
                return false;
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof MD5) {
                return MD5.byteEquals(this._digest, ((MD5)obj)._digest);
            }
            return false;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public static String digest(String password) {
            try {
                byte[] digest;
                Object object = __md5Lock;
                synchronized (object) {
                    if (__md == null) {
                        try {
                            __md = MessageDigest.getInstance("MD5");
                        }
                        catch (Exception e) {
                            LOG.warn(e);
                            return null;
                        }
                    }
                    __md.reset();
                    __md.update(password.getBytes(StandardCharsets.ISO_8859_1));
                    digest = __md.digest();
                }
                return __TYPE + TypeUtil.toString(digest, 16);
            }
            catch (Exception e) {
                LOG.warn(e);
                return null;
            }
        }
    }

    public static class Crypt
    extends Credential {
        private static final long serialVersionUID = -2027792997664744210L;
        private static final String __TYPE = "CRYPT:";
        private final String _cooked;

        Crypt(String cooked) {
            this._cooked = cooked.startsWith(__TYPE) ? cooked.substring(__TYPE.length()) : cooked;
        }

        @Override
        public boolean check(Object credentials) {
            if (credentials instanceof char[]) {
                credentials = new String((char[])credentials);
            }
            if (!(credentials instanceof String) && !(credentials instanceof Password)) {
                LOG.warn("Can't check " + credentials.getClass() + " against CRYPT", new Object[0]);
            }
            return Crypt.stringEquals(this._cooked, UnixCrypt.crypt(credentials.toString(), this._cooked));
        }

        public boolean equals(Object credential) {
            if (!(credential instanceof Crypt)) {
                return false;
            }
            Crypt c = (Crypt)credential;
            return Crypt.stringEquals(this._cooked, c._cooked);
        }

        public static String crypt(String user, String pw) {
            return __TYPE + UnixCrypt.crypt(pw, user);
        }
    }
}

