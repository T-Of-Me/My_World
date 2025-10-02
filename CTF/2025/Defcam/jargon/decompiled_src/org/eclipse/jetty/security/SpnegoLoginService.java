/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.util.Properties;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SpnegoUserPrincipal;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

public class SpnegoLoginService
extends AbstractLifeCycle
implements LoginService {
    private static final Logger LOG = Log.getLogger(SpnegoLoginService.class);
    protected IdentityService _identityService;
    protected String _name;
    private String _config;
    private String _targetName;

    public SpnegoLoginService() {
    }

    public SpnegoLoginService(String name) {
        this.setName(name);
    }

    public SpnegoLoginService(String name, String config) {
        this.setName(name);
        this.setConfig(config);
    }

    @Override
    public String getName() {
        return this._name;
    }

    public void setName(String name) {
        if (this.isRunning()) {
            throw new IllegalStateException("Running");
        }
        this._name = name;
    }

    public String getConfig() {
        return this._config;
    }

    public void setConfig(String config) {
        if (this.isRunning()) {
            throw new IllegalStateException("Running");
        }
        this._config = config;
    }

    @Override
    protected void doStart() throws Exception {
        Properties properties = new Properties();
        Resource resource = Resource.newResource(this._config);
        properties.load(resource.getInputStream());
        this._targetName = properties.getProperty("targetName");
        LOG.debug("Target Name {}", this._targetName);
        super.doStart();
    }

    @Override
    public UserIdentity login(String username, Object credentials, ServletRequest request) {
        String encodedAuthToken = (String)credentials;
        byte[] authToken = B64Code.decode(encodedAuthToken);
        GSSManager manager = GSSManager.getInstance();
        try {
            Oid krb5Oid = new Oid("1.3.6.1.5.5.2");
            GSSName gssName = manager.createName(this._targetName, null);
            GSSCredential serverCreds = manager.createCredential(gssName, Integer.MAX_VALUE, krb5Oid, 2);
            GSSContext gContext = manager.createContext(serverCreds);
            if (gContext == null) {
                LOG.debug("SpnegoUserRealm: failed to establish GSSContext", new Object[0]);
            } else {
                while (!gContext.isEstablished()) {
                    authToken = gContext.acceptSecContext(authToken, 0, authToken.length);
                }
                if (gContext.isEstablished()) {
                    String clientName = gContext.getSrcName().toString();
                    String role = clientName.substring(clientName.indexOf(64) + 1);
                    LOG.debug("SpnegoUserRealm: established a security context", new Object[0]);
                    LOG.debug("Client Principal is: " + gContext.getSrcName(), new Object[0]);
                    LOG.debug("Server Principal is: " + gContext.getTargName(), new Object[0]);
                    LOG.debug("Client Default Role: " + role, new Object[0]);
                    SpnegoUserPrincipal user = new SpnegoUserPrincipal(clientName, authToken);
                    Subject subject = new Subject();
                    subject.getPrincipals().add(user);
                    return this._identityService.newUserIdentity(subject, user, new String[]{role});
                }
            }
        }
        catch (GSSException gsse) {
            LOG.warn(gsse);
        }
        return null;
    }

    @Override
    public boolean validate(UserIdentity user) {
        return false;
    }

    @Override
    public IdentityService getIdentityService() {
        return this._identityService;
    }

    @Override
    public void setIdentityService(IdentityService service) {
        this._identityService = service;
    }

    @Override
    public void logout(UserIdentity user) {
    }
}

