/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import org.eclipse.jetty.util.resource.Resource;

public class CertificateUtils {
    public static KeyStore getKeyStore(Resource store, String storeType, String storeProvider, String storePassword) throws Exception {
        KeyStore keystore = null;
        if (store != null) {
            keystore = storeProvider != null ? KeyStore.getInstance(storeType, storeProvider) : KeyStore.getInstance(storeType);
            if (!store.exists()) {
                throw new IllegalStateException("no valid keystore");
            }
            try (InputStream inStream = store.getInputStream();){
                keystore.load(inStream, storePassword == null ? null : storePassword.toCharArray());
            }
        }
        return keystore;
    }

    public static Collection<? extends CRL> loadCRL(String crlPath) throws Exception {
        Collection<? extends CRL> crlList = null;
        if (crlPath != null) {
            try (InputStream in = null;){
                in = Resource.newResource(crlPath).getInputStream();
                crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
            }
        }
        return crlList;
    }
}

