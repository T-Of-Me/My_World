/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.ssl;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class X509 {
    private static final Logger LOG = Log.getLogger(X509.class);
    private static final int KEY_USAGE__KEY_CERT_SIGN = 5;
    private static final int SUBJECT_ALTERNATIVE_NAMES__DNS_NAME = 2;
    private final X509Certificate _x509;
    private final String _alias;
    private final List<String> _hosts = new ArrayList<String>();
    private final List<String> _wilds = new ArrayList<String>();

    public static boolean isCertSign(X509Certificate x509) {
        boolean[] key_usage = x509.getKeyUsage();
        if (key_usage == null || key_usage.length <= 5) {
            return false;
        }
        return key_usage[5];
    }

    public X509(String alias, X509Certificate x509) throws CertificateParsingException, InvalidNameException {
        this._alias = alias;
        this._x509 = x509;
        boolean named = false;
        Collection<List<?>> altNames = x509.getSubjectAlternativeNames();
        if (altNames != null) {
            for (List<?> list : altNames) {
                if (((Number)list.get(0)).intValue() != 2) continue;
                String cn = list.get(1).toString();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Certificate SAN alias={} CN={} in {}", alias, cn, this);
                }
                if (cn == null) continue;
                named = true;
                this.addName(cn);
            }
        }
        if (!named) {
            LdapName name = new LdapName(x509.getSubjectX500Principal().getName("RFC2253"));
            for (Rdn rdn : name.getRdns()) {
                if (!rdn.getType().equalsIgnoreCase("CN")) continue;
                String cn = rdn.getValue().toString();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Certificate CN alias={} CN={} in {}", alias, cn, this);
                }
                if (cn == null || !cn.contains(".") || cn.contains(" ")) continue;
                this.addName(cn);
            }
        }
    }

    protected void addName(String cn) {
        if ((cn = StringUtil.asciiToLowerCase(cn)).startsWith("*.")) {
            this._wilds.add(cn.substring(2));
        } else {
            this._hosts.add(cn);
        }
    }

    public String getAlias() {
        return this._alias;
    }

    public X509Certificate getCertificate() {
        return this._x509;
    }

    public Set<String> getHosts() {
        return new HashSet<String>(this._hosts);
    }

    public Set<String> getWilds() {
        return new HashSet<String>(this._wilds);
    }

    public boolean matches(String host) {
        String domain;
        if (this._hosts.contains(host = StringUtil.asciiToLowerCase(host)) || this._wilds.contains(host)) {
            return true;
        }
        int dot = host.indexOf(46);
        return dot >= 0 && this._wilds.contains(domain = host.substring(dot + 1));
    }

    public String toString() {
        return String.format("%s@%x(%s,h=%s,w=%s)", this.getClass().getSimpleName(), this.hashCode(), this._alias, this._hosts, this._wilds);
    }
}

