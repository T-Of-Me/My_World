/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.security;

import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathValidator;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class CertificateValidator {
    private static final Logger LOG = Log.getLogger(CertificateValidator.class);
    private static AtomicLong __aliasCount = new AtomicLong();
    private KeyStore _trustStore;
    private Collection<? extends CRL> _crls;
    private int _maxCertPathLength = -1;
    private boolean _enableCRLDP = false;
    private boolean _enableOCSP = false;
    private String _ocspResponderURL;

    public CertificateValidator(KeyStore trustStore, Collection<? extends CRL> crls) {
        if (trustStore == null) {
            throw new InvalidParameterException("TrustStore must be specified for CertificateValidator.");
        }
        this._trustStore = trustStore;
        this._crls = crls;
    }

    public void validate(KeyStore keyStore) throws CertificateException {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                this.validate(keyStore, alias);
            }
        }
        catch (KeyStoreException kse) {
            throw new CertificateException("Unable to retrieve aliases from keystore", kse);
        }
    }

    public String validate(KeyStore keyStore, String keyAlias) throws CertificateException {
        String result = null;
        if (keyAlias != null) {
            try {
                this.validate(keyStore, keyStore.getCertificate(keyAlias));
            }
            catch (KeyStoreException kse) {
                LOG.debug(kse);
                throw new CertificateException("Unable to validate certificate for alias [" + keyAlias + "]: " + kse.getMessage(), kse);
            }
            result = keyAlias;
        }
        return result;
    }

    public void validate(KeyStore keyStore, Certificate cert) throws CertificateException {
        Certificate[] certChain = null;
        if (cert != null && cert instanceof X509Certificate) {
            ((X509Certificate)cert).checkValidity();
            String certAlias = null;
            try {
                if (keyStore == null) {
                    throw new InvalidParameterException("Keystore cannot be null");
                }
                certAlias = keyStore.getCertificateAlias((X509Certificate)cert);
                if (certAlias == null) {
                    certAlias = "JETTY" + String.format("%016X", __aliasCount.incrementAndGet());
                    keyStore.setCertificateEntry(certAlias, cert);
                }
                if ((certChain = keyStore.getCertificateChain(certAlias)) == null || certChain.length == 0) {
                    throw new IllegalStateException("Unable to retrieve certificate chain");
                }
            }
            catch (KeyStoreException kse) {
                LOG.debug(kse);
                throw new CertificateException("Unable to validate certificate" + (certAlias == null ? "" : " for alias [" + certAlias + "]") + ": " + kse.getMessage(), kse);
            }
            this.validate(certChain);
        }
    }

    public void validate(Certificate[] certChain) throws CertificateException {
        try {
            ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
            for (Certificate item : certChain) {
                if (item == null) continue;
                if (!(item instanceof X509Certificate)) {
                    throw new IllegalStateException("Invalid certificate type in chain");
                }
                certList.add((X509Certificate)item);
            }
            if (certList.isEmpty()) {
                throw new IllegalStateException("Invalid certificate chain");
            }
            X509CertSelector certSelect = new X509CertSelector();
            certSelect.setCertificate((X509Certificate)certList.get(0));
            PKIXBuilderParameters pbParams = new PKIXBuilderParameters(this._trustStore, (CertSelector)certSelect);
            pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList)));
            pbParams.setMaxPathLength(this._maxCertPathLength);
            pbParams.setRevocationEnabled(true);
            if (this._crls != null && !this._crls.isEmpty()) {
                pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(this._crls)));
            }
            if (this._enableOCSP) {
                Security.setProperty("ocsp.enable", "true");
            }
            if (this._enableCRLDP) {
                System.setProperty("com.sun.security.enableCRLDP", "true");
            }
            CertPathBuilderResult buildResult = CertPathBuilder.getInstance("PKIX").build(pbParams);
            CertPathValidator.getInstance("PKIX").validate(buildResult.getCertPath(), pbParams);
        }
        catch (GeneralSecurityException gse) {
            LOG.debug(gse);
            throw new CertificateException("Unable to validate certificate: " + gse.getMessage(), gse);
        }
    }

    public KeyStore getTrustStore() {
        return this._trustStore;
    }

    public Collection<? extends CRL> getCrls() {
        return this._crls;
    }

    public int getMaxCertPathLength() {
        return this._maxCertPathLength;
    }

    public void setMaxCertPathLength(int maxCertPathLength) {
        this._maxCertPathLength = maxCertPathLength;
    }

    public boolean isEnableCRLDP() {
        return this._enableCRLDP;
    }

    public void setEnableCRLDP(boolean enableCRLDP) {
        this._enableCRLDP = enableCRLDP;
    }

    public boolean isEnableOCSP() {
        return this._enableOCSP;
    }

    public void setEnableOCSP(boolean enableOCSP) {
        this._enableOCSP = enableOCSP;
    }

    public String getOcspResponderURL() {
        return this._ocspResponderURL;
    }

    public void setOcspResponderURL(String ocspResponderURL) {
        this._ocspResponderURL = ocspResponderURL;
    }
}

