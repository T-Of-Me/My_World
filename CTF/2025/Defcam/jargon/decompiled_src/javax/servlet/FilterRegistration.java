/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.Collection;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Registration;

public interface FilterRegistration
extends Registration {
    public void addMappingForServletNames(EnumSet<DispatcherType> var1, boolean var2, String ... var3);

    public Collection<String> getServletNameMappings();

    public void addMappingForUrlPatterns(EnumSet<DispatcherType> var1, boolean var2, String ... var3);

    public Collection<String> getUrlPatternMappings();

    public static interface Dynamic
    extends FilterRegistration,
    Registration.Dynamic {
    }
}

