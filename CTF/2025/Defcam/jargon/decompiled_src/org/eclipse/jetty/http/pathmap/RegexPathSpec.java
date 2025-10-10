/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http.pathmap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http.pathmap.PathSpecGroup;

public class RegexPathSpec
extends PathSpec {
    protected Pattern pattern;

    protected RegexPathSpec() {
    }

    public RegexPathSpec(String regex) {
        this.pathSpec = regex;
        if (regex.startsWith("regex|")) {
            this.pathSpec = regex.substring("regex|".length());
        }
        this.pathDepth = 0;
        this.specLength = this.pathSpec.length();
        boolean inGrouping = false;
        StringBuilder signature = new StringBuilder();
        block6: for (char c : this.pathSpec.toCharArray()) {
            switch (c) {
                case '[': {
                    inGrouping = true;
                    continue block6;
                }
                case ']': {
                    inGrouping = false;
                    signature.append('g');
                    continue block6;
                }
                case '*': {
                    signature.append('g');
                    continue block6;
                }
                case '/': {
                    if (inGrouping) continue block6;
                    ++this.pathDepth;
                    continue block6;
                }
                default: {
                    if (inGrouping || !Character.isLetterOrDigit(c)) continue block6;
                    signature.append('l');
                }
            }
        }
        this.pattern = Pattern.compile(this.pathSpec);
        String sig = signature.toString();
        this.group = Pattern.matches("^l*$", sig) ? PathSpecGroup.EXACT : (Pattern.matches("^l*g+", sig) ? PathSpecGroup.PREFIX_GLOB : (Pattern.matches("^g+l+$", sig) ? PathSpecGroup.SUFFIX_GLOB : PathSpecGroup.MIDDLE_GLOB));
    }

    public Matcher getMatcher(String path) {
        return this.pattern.matcher(path);
    }

    @Override
    public String getPathInfo(String path) {
        Matcher matcher;
        if (this.group == PathSpecGroup.PREFIX_GLOB && (matcher = this.getMatcher(path)).matches() && matcher.groupCount() >= 1) {
            String pathInfo = matcher.group(1);
            if ("".equals(pathInfo)) {
                return "/";
            }
            return pathInfo;
        }
        return null;
    }

    @Override
    public String getPathMatch(String path) {
        Matcher matcher = this.getMatcher(path);
        if (matcher.matches()) {
            int idx;
            if (matcher.groupCount() >= 1 && (idx = matcher.start(1)) > 0) {
                if (path.charAt(idx - 1) == '/') {
                    --idx;
                }
                return path.substring(0, idx);
            }
            return path;
        }
        return null;
    }

    public Pattern getPattern() {
        return this.pattern;
    }

    @Override
    public String getRelativePath(String base, String path) {
        return null;
    }

    @Override
    public boolean matches(String path) {
        int idx = path.indexOf(63);
        if (idx >= 0) {
            return this.getMatcher(path.substring(0, idx)).matches();
        }
        return this.getMatcher(path).matches();
    }
}

