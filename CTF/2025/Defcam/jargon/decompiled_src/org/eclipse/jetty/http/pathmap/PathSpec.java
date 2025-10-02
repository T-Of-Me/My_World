/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http.pathmap;

import org.eclipse.jetty.http.pathmap.PathSpecGroup;

public abstract class PathSpec
implements Comparable<PathSpec> {
    protected String pathSpec;
    protected PathSpecGroup group;
    protected int pathDepth;
    protected int specLength;
    protected String prefix;
    protected String suffix;

    @Override
    public int compareTo(PathSpec other) {
        int diff = this.group.ordinal() - other.group.ordinal();
        if (diff != 0) {
            return diff;
        }
        diff = other.specLength - this.specLength;
        if (diff != 0) {
            return diff;
        }
        return this.pathSpec.compareTo(other.pathSpec);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        PathSpec other = (PathSpec)obj;
        return !(this.pathSpec == null ? other.pathSpec != null : !this.pathSpec.equals(other.pathSpec));
    }

    public PathSpecGroup getGroup() {
        return this.group;
    }

    public int getPathDepth() {
        return this.pathDepth;
    }

    public abstract String getPathInfo(String var1);

    public abstract String getPathMatch(String var1);

    public String getDeclaration() {
        return this.pathSpec;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public abstract String getRelativePath(String var1, String var2);

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.pathSpec == null ? 0 : this.pathSpec.hashCode());
        return result;
    }

    public abstract boolean matches(String var1);

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(this.getClass().getSimpleName()).append("[\"");
        str.append(this.pathSpec);
        str.append("\",pathDepth=").append(this.pathDepth);
        str.append(",group=").append((Object)this.group);
        str.append("]");
        return str.toString();
    }
}

