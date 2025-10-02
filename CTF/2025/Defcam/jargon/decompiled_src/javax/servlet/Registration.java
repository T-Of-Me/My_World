/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import java.util.Map;
import java.util.Set;

public interface Registration {
    public String getName();

    public String getClassName();

    public boolean setInitParameter(String var1, String var2);

    public String getInitParameter(String var1);

    public Set<String> setInitParameters(Map<String, String> var1);

    public Map<String, String> getInitParameters();

    public static interface Dynamic
    extends Registration {
        public void setAsyncSupported(boolean var1);
    }
}

