/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.security.ConstraintAware;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.RoleInfo;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserDataConstraint;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Constraint;

public class ConstraintSecurityHandler
extends SecurityHandler
implements ConstraintAware {
    private static final Logger LOG = Log.getLogger(SecurityHandler.class);
    private static final String OMISSION_SUFFIX = ".omission";
    private static final String ALL_METHODS = "*";
    private final List<ConstraintMapping> _constraintMappings = new CopyOnWriteArrayList<ConstraintMapping>();
    private final Set<String> _roles = new CopyOnWriteArraySet<String>();
    private final PathMap<Map<String, RoleInfo>> _constraintMap = new PathMap();
    private boolean _denyUncoveredMethods = false;

    public static Constraint createConstraint() {
        return new Constraint();
    }

    public static Constraint createConstraint(Constraint constraint) {
        try {
            return (Constraint)constraint.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Constraint createConstraint(String name, boolean authenticate, String[] roles, int dataConstraint) {
        Constraint constraint = ConstraintSecurityHandler.createConstraint();
        if (name != null) {
            constraint.setName(name);
        }
        constraint.setAuthenticate(authenticate);
        constraint.setRoles(roles);
        constraint.setDataConstraint(dataConstraint);
        return constraint;
    }

    public static Constraint createConstraint(String name, HttpConstraintElement element) {
        return ConstraintSecurityHandler.createConstraint(name, element.getRolesAllowed(), element.getEmptyRoleSemantic(), element.getTransportGuarantee());
    }

    public static Constraint createConstraint(String name, String[] rolesAllowed, ServletSecurity.EmptyRoleSemantic permitOrDeny, ServletSecurity.TransportGuarantee transport) {
        Constraint constraint = ConstraintSecurityHandler.createConstraint();
        if (rolesAllowed == null || rolesAllowed.length == 0) {
            if (permitOrDeny.equals((Object)ServletSecurity.EmptyRoleSemantic.DENY)) {
                constraint.setName(name + "-Deny");
                constraint.setAuthenticate(true);
            } else {
                constraint.setName(name + "-Permit");
                constraint.setAuthenticate(false);
            }
        } else {
            constraint.setAuthenticate(true);
            constraint.setRoles(rolesAllowed);
            constraint.setName(name + "-RolesAllowed");
        }
        constraint.setDataConstraint(transport.equals((Object)ServletSecurity.TransportGuarantee.CONFIDENTIAL) ? 2 : 0);
        return constraint;
    }

    public static List<ConstraintMapping> getConstraintMappingsForPath(String pathSpec, List<ConstraintMapping> constraintMappings) {
        if (pathSpec == null || "".equals(pathSpec.trim()) || constraintMappings == null || constraintMappings.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<ConstraintMapping> mappings = new ArrayList<ConstraintMapping>();
        for (ConstraintMapping mapping : constraintMappings) {
            if (!pathSpec.equals(mapping.getPathSpec())) continue;
            mappings.add(mapping);
        }
        return mappings;
    }

    public static List<ConstraintMapping> removeConstraintMappingsForPath(String pathSpec, List<ConstraintMapping> constraintMappings) {
        if (pathSpec == null || "".equals(pathSpec.trim()) || constraintMappings == null || constraintMappings.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<ConstraintMapping> mappings = new ArrayList<ConstraintMapping>();
        for (ConstraintMapping mapping : constraintMappings) {
            if (pathSpec.equals(mapping.getPathSpec())) continue;
            mappings.add(mapping);
        }
        return mappings;
    }

    public static List<ConstraintMapping> createConstraintsWithMappingsForPath(String name, String pathSpec, ServletSecurityElement securityElement) {
        ArrayList<ConstraintMapping> mappings = new ArrayList<ConstraintMapping>();
        Constraint httpConstraint = null;
        ConstraintMapping httpConstraintMapping = null;
        if (securityElement.getEmptyRoleSemantic() != ServletSecurity.EmptyRoleSemantic.PERMIT || securityElement.getRolesAllowed().length != 0 || securityElement.getTransportGuarantee() != ServletSecurity.TransportGuarantee.NONE) {
            httpConstraint = ConstraintSecurityHandler.createConstraint(name, securityElement);
            httpConstraintMapping = new ConstraintMapping();
            httpConstraintMapping.setPathSpec(pathSpec);
            httpConstraintMapping.setConstraint(httpConstraint);
            mappings.add(httpConstraintMapping);
        }
        ArrayList<String> methodOmissions = new ArrayList<String>();
        Collection<HttpMethodConstraintElement> methodConstraintElements = securityElement.getHttpMethodConstraints();
        if (methodConstraintElements != null) {
            for (HttpMethodConstraintElement methodConstraintElement : methodConstraintElements) {
                Constraint methodConstraint = ConstraintSecurityHandler.createConstraint(name, methodConstraintElement);
                ConstraintMapping mapping = new ConstraintMapping();
                mapping.setConstraint(methodConstraint);
                mapping.setPathSpec(pathSpec);
                if (methodConstraintElement.getMethodName() != null) {
                    mapping.setMethod(methodConstraintElement.getMethodName());
                    methodOmissions.add(methodConstraintElement.getMethodName());
                }
                mappings.add(mapping);
            }
        }
        if (methodOmissions.size() > 0 && httpConstraintMapping != null) {
            httpConstraintMapping.setMethodOmissions(methodOmissions.toArray(new String[methodOmissions.size()]));
        }
        return mappings;
    }

    @Override
    public List<ConstraintMapping> getConstraintMappings() {
        return this._constraintMappings;
    }

    @Override
    public Set<String> getRoles() {
        return this._roles;
    }

    public void setConstraintMappings(List<ConstraintMapping> constraintMappings) {
        this.setConstraintMappings(constraintMappings, null);
    }

    public void setConstraintMappings(ConstraintMapping[] constraintMappings) {
        this.setConstraintMappings(Arrays.asList(constraintMappings), null);
    }

    @Override
    public void setConstraintMappings(List<ConstraintMapping> constraintMappings, Set<String> roles) {
        this._constraintMappings.clear();
        this._constraintMappings.addAll(constraintMappings);
        if (roles == null) {
            roles = new HashSet<String>();
            for (ConstraintMapping cm : constraintMappings) {
                String[] cmr = cm.getConstraint().getRoles();
                if (cmr == null) continue;
                for (String r : cmr) {
                    if (ALL_METHODS.equals(r)) continue;
                    roles.add(r);
                }
            }
        }
        this.setRoles(roles);
        if (this.isStarted()) {
            for (ConstraintMapping mapping : this._constraintMappings) {
                this.processConstraintMapping(mapping);
            }
        }
    }

    public void setRoles(Set<String> roles) {
        this._roles.clear();
        this._roles.addAll(roles);
    }

    @Override
    public void addConstraintMapping(ConstraintMapping mapping) {
        this._constraintMappings.add(mapping);
        if (mapping.getConstraint() != null && mapping.getConstraint().getRoles() != null) {
            for (String role : mapping.getConstraint().getRoles()) {
                if (ALL_METHODS.equals(role) || "**".equals(role)) continue;
                this.addRole(role);
            }
        }
        if (this.isStarted()) {
            this.processConstraintMapping(mapping);
        }
    }

    @Override
    public void addRole(String role) {
        boolean modified = this._roles.add(role);
        if (this.isStarted() && modified) {
            for (Map map : this._constraintMap.values()) {
                for (RoleInfo info : map.values()) {
                    if (!info.isAnyRole()) continue;
                    info.addRole(role);
                }
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        this._constraintMap.clear();
        if (this._constraintMappings != null) {
            for (ConstraintMapping mapping : this._constraintMappings) {
                this.processConstraintMapping(mapping);
            }
        }
        this.checkPathsWithUncoveredHttpMethods();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this._constraintMap.clear();
    }

    protected void processConstraintMapping(ConstraintMapping mapping) {
        RoleInfo roleInfo;
        RoleInfo allMethodsRoleInfo;
        HashMap<String, RoleInfo> mappings = (HashMap<String, RoleInfo>)this._constraintMap.get(mapping.getPathSpec());
        if (mappings == null) {
            mappings = new HashMap<String, RoleInfo>();
            this._constraintMap.put(mapping.getPathSpec(), (Map<String, RoleInfo>)mappings);
        }
        if ((allMethodsRoleInfo = (RoleInfo)mappings.get(ALL_METHODS)) != null && allMethodsRoleInfo.isForbidden()) {
            return;
        }
        if (mapping.getMethodOmissions() != null && mapping.getMethodOmissions().length > 0) {
            this.processConstraintMappingWithMethodOmissions(mapping, mappings);
            return;
        }
        String httpMethod = mapping.getMethod();
        if (httpMethod == null) {
            httpMethod = ALL_METHODS;
        }
        if ((roleInfo = (RoleInfo)mappings.get(httpMethod)) == null) {
            roleInfo = new RoleInfo();
            mappings.put(httpMethod, roleInfo);
            if (allMethodsRoleInfo != null) {
                roleInfo.combine(allMethodsRoleInfo);
            }
        }
        if (roleInfo.isForbidden()) {
            return;
        }
        this.configureRoleInfo(roleInfo, mapping);
        if (roleInfo.isForbidden() && httpMethod.equals(ALL_METHODS)) {
            mappings.clear();
            mappings.put(ALL_METHODS, roleInfo);
        }
    }

    protected void processConstraintMappingWithMethodOmissions(ConstraintMapping mapping, Map<String, RoleInfo> mappings) {
        String[] omissions = mapping.getMethodOmissions();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < omissions.length; ++i) {
            if (i > 0) {
                sb.append(".");
            }
            sb.append(omissions[i]);
        }
        sb.append(OMISSION_SUFFIX);
        RoleInfo ri = new RoleInfo();
        mappings.put(sb.toString(), ri);
        this.configureRoleInfo(ri, mapping);
    }

    protected void configureRoleInfo(RoleInfo ri, ConstraintMapping mapping) {
        Constraint constraint = mapping.getConstraint();
        boolean forbidden = constraint.isForbidden();
        ri.setForbidden(forbidden);
        UserDataConstraint userDataConstraint = UserDataConstraint.get(mapping.getConstraint().getDataConstraint());
        ri.setUserDataConstraint(userDataConstraint);
        if (!ri.isForbidden()) {
            boolean checked = mapping.getConstraint().getAuthenticate();
            ri.setChecked(checked);
            if (ri.isChecked()) {
                if (mapping.getConstraint().isAnyRole()) {
                    for (String role : this._roles) {
                        ri.addRole(role);
                    }
                    ri.setAnyRole(true);
                } else if (mapping.getConstraint().isAnyAuth()) {
                    ri.setAnyAuth(true);
                } else {
                    String[] newRoles;
                    for (String role : newRoles = mapping.getConstraint().getRoles()) {
                        if (!this._roles.contains(role)) {
                            throw new IllegalArgumentException("Attempt to use undeclared role: " + role + ", known roles: " + this._roles);
                        }
                        ri.addRole(role);
                    }
                }
            }
        }
    }

    @Override
    protected RoleInfo prepareConstraintInfo(String pathInContext, Request request) {
        Map<String, RoleInfo> mappings = this._constraintMap.match(pathInContext);
        if (mappings != null) {
            String httpMethod = request.getMethod();
            RoleInfo roleInfo = mappings.get(httpMethod);
            if (roleInfo == null) {
                ArrayList<RoleInfo> applicableConstraints = new ArrayList<RoleInfo>();
                RoleInfo all = mappings.get(ALL_METHODS);
                if (all != null) {
                    applicableConstraints.add(all);
                }
                for (Map.Entry<String, RoleInfo> entry : mappings.entrySet()) {
                    if (entry.getKey() == null || !entry.getKey().endsWith(OMISSION_SUFFIX) || entry.getKey().contains(httpMethod)) continue;
                    applicableConstraints.add(entry.getValue());
                }
                if (applicableConstraints.size() == 0 && this.isDenyUncoveredHttpMethods()) {
                    roleInfo = new RoleInfo();
                    roleInfo.setForbidden(true);
                } else if (applicableConstraints.size() == 1) {
                    roleInfo = (RoleInfo)applicableConstraints.get(0);
                } else {
                    roleInfo = new RoleInfo();
                    roleInfo.setUserDataConstraint(UserDataConstraint.None);
                    for (RoleInfo r : applicableConstraints) {
                        roleInfo.combine(r);
                    }
                }
            }
            return roleInfo;
        }
        return null;
    }

    @Override
    protected boolean checkUserDataPermissions(String pathInContext, Request request, Response response, RoleInfo roleInfo) throws IOException {
        if (roleInfo == null) {
            return true;
        }
        if (roleInfo.isForbidden()) {
            return false;
        }
        UserDataConstraint dataConstraint = roleInfo.getUserDataConstraint();
        if (dataConstraint == null || dataConstraint == UserDataConstraint.None) {
            return true;
        }
        HttpConfiguration httpConfig = Request.getBaseRequest(request).getHttpChannel().getHttpConfiguration();
        if (dataConstraint == UserDataConstraint.Confidential || dataConstraint == UserDataConstraint.Integral) {
            if (request.isSecure()) {
                return true;
            }
            if (httpConfig.getSecurePort() > 0) {
                String scheme = httpConfig.getSecureScheme();
                int port = httpConfig.getSecurePort();
                String url = URIUtil.newURI(scheme, request.getServerName(), port, request.getRequestURI(), request.getQueryString());
                response.setContentLength(0);
                response.sendRedirect(url);
            } else {
                response.sendError(403, "!Secure");
            }
            request.setHandled(true);
            return false;
        }
        throw new IllegalArgumentException("Invalid dataConstraint value: " + (Object)((Object)dataConstraint));
    }

    @Override
    protected boolean isAuthMandatory(Request baseRequest, Response base_response, Object constraintInfo) {
        return constraintInfo != null && ((RoleInfo)constraintInfo).isChecked();
    }

    @Override
    protected boolean checkWebResourcePermissions(String pathInContext, Request request, Response response, Object constraintInfo, UserIdentity userIdentity) throws IOException {
        if (constraintInfo == null) {
            return true;
        }
        RoleInfo roleInfo = (RoleInfo)constraintInfo;
        if (!roleInfo.isChecked()) {
            return true;
        }
        if (roleInfo.isAnyAuth() && request.getUserPrincipal() != null) {
            return true;
        }
        boolean isUserInRole = false;
        for (String role : roleInfo.getRoles()) {
            if (!userIdentity.isUserInRole(role, null)) continue;
            isUserInRole = true;
            break;
        }
        if (roleInfo.isAnyRole() && request.getUserPrincipal() != null && isUserInRole) {
            return true;
        }
        return isUserInRole;
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        this.dumpBeans(out, indent, Collections.singleton(this.getLoginService()), Collections.singleton(this.getIdentityService()), Collections.singleton(this.getAuthenticator()), Collections.singleton(this._roles), this._constraintMap.entrySet());
    }

    @Override
    public void setDenyUncoveredHttpMethods(boolean deny) {
        this._denyUncoveredMethods = deny;
    }

    @Override
    public boolean isDenyUncoveredHttpMethods() {
        return this._denyUncoveredMethods;
    }

    @Override
    public boolean checkPathsWithUncoveredHttpMethods() {
        Set<String> paths = this.getPathsWithUncoveredHttpMethods();
        if (paths != null && !paths.isEmpty()) {
            for (String p : paths) {
                LOG.warn("{} has uncovered http methods for path: {}", ContextHandler.getCurrentContext(), p);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(new Throwable());
            }
            return true;
        }
        return false;
    }

    public Set<String> getPathsWithUncoveredHttpMethods() {
        if (this._denyUncoveredMethods) {
            return Collections.emptySet();
        }
        HashSet<String> uncoveredPaths = new HashSet<String>();
        for (String path : this._constraintMap.keySet()) {
            Map methodMappings = (Map)this._constraintMap.get(path);
            if (methodMappings.get(ALL_METHODS) != null) continue;
            boolean hasOmissions = this.omissionsExist(path, methodMappings);
            for (String method : methodMappings.keySet()) {
                if (method.endsWith(OMISSION_SUFFIX)) {
                    Set<String> omittedMethods = this.getOmittedMethods(method);
                    for (String m : omittedMethods) {
                        if (methodMappings.containsKey(m)) continue;
                        uncoveredPaths.add(path);
                    }
                    continue;
                }
                if (hasOmissions) continue;
                uncoveredPaths.add(path);
            }
        }
        return uncoveredPaths;
    }

    protected boolean omissionsExist(String path, Map<String, RoleInfo> methodMappings) {
        if (methodMappings == null) {
            return false;
        }
        boolean hasOmissions = false;
        for (String m : methodMappings.keySet()) {
            if (!m.endsWith(OMISSION_SUFFIX)) continue;
            hasOmissions = true;
        }
        return hasOmissions;
    }

    protected Set<String> getOmittedMethods(String omission) {
        if (omission == null || !omission.endsWith(OMISSION_SUFFIX)) {
            return Collections.emptySet();
        }
        String[] strings = omission.split("\\.");
        HashSet<String> methods = new HashSet<String>();
        for (int i = 0; i < strings.length - 1; ++i) {
            methods.add(strings[i]);
        }
        return methods;
    }
}

