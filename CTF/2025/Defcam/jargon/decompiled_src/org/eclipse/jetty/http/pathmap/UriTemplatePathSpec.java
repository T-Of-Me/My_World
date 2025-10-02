/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.http.pathmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jetty.http.pathmap.PathSpecGroup;
import org.eclipse.jetty.http.pathmap.RegexPathSpec;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class UriTemplatePathSpec
extends RegexPathSpec {
    private static final Logger LOG = Log.getLogger(UriTemplatePathSpec.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(.*)\\}");
    private static final String VARIABLE_RESERVED = ":/?#[]@!$&'()*+,;=";
    private static final String VARIABLE_SYMBOLS = "-._";
    private static final Set<String> FORBIDDEN_SEGMENTS = new HashSet<String>();
    private String[] variables;

    public UriTemplatePathSpec(String rawSpec) {
        Objects.requireNonNull(rawSpec, "Path Param Spec cannot be null");
        if ("".equals(rawSpec) || "/".equals(rawSpec)) {
            this.pathSpec = "/";
            this.pattern = Pattern.compile("^/$");
            this.pathDepth = 1;
            this.specLength = 1;
            this.variables = new String[0];
            this.group = PathSpecGroup.EXACT;
            return;
        }
        if (rawSpec.charAt(0) != '/') {
            StringBuilder err = new StringBuilder();
            err.append("Syntax Error: path spec \"");
            err.append(rawSpec);
            err.append("\" must start with '/'");
            throw new IllegalArgumentException(err.toString());
        }
        for (String forbidden : FORBIDDEN_SEGMENTS) {
            if (!rawSpec.contains(forbidden)) continue;
            StringBuilder err = new StringBuilder();
            err.append("Syntax Error: segment ");
            err.append(forbidden);
            err.append(" is forbidden in path spec: ");
            err.append(rawSpec);
            throw new IllegalArgumentException(err.toString());
        }
        this.pathSpec = rawSpec;
        StringBuilder regex = new StringBuilder();
        regex.append('^');
        ArrayList<String> varNames = new ArrayList<String>();
        String[] segments = rawSpec.substring(1).split("/");
        char[] segmentSignature = new char[segments.length];
        this.pathDepth = segments.length;
        for (int i = 0; i < segments.length; ++i) {
            StringBuilder err;
            String segment = segments[i];
            Matcher mat = VARIABLE_PATTERN.matcher(segment);
            if (mat.matches()) {
                String variable = mat.group(1);
                if (varNames.contains(variable)) {
                    StringBuilder err2 = new StringBuilder();
                    err2.append("Syntax Error: variable ");
                    err2.append(variable);
                    err2.append(" is duplicated in path spec: ");
                    err2.append(rawSpec);
                    throw new IllegalArgumentException(err2.toString());
                }
                this.assertIsValidVariableLiteral(variable);
                segmentSignature[i] = 118;
                varNames.add(variable);
                regex.append("/([^/]+)");
                continue;
            }
            if (mat.find(0)) {
                err = new StringBuilder();
                err.append("Syntax Error: variable ");
                err.append(mat.group());
                err.append(" must exist as entire path segment: ");
                err.append(rawSpec);
                throw new IllegalArgumentException(err.toString());
            }
            if (segment.indexOf(123) >= 0 || segment.indexOf(125) >= 0) {
                err = new StringBuilder();
                err.append("Syntax Error: invalid path segment /");
                err.append(segment);
                err.append("/ variable declaration incomplete: ");
                err.append(rawSpec);
                throw new IllegalArgumentException(err.toString());
            }
            if (segment.indexOf(42) >= 0) {
                err = new StringBuilder();
                err.append("Syntax Error: path segment /");
                err.append(segment);
                err.append("/ contains a wildcard symbol (not supported by this uri-template implementation): ");
                err.append(rawSpec);
                throw new IllegalArgumentException(err.toString());
            }
            segmentSignature[i] = 101;
            regex.append('/');
            for (char c : segment.toCharArray()) {
                if (c == '.' || c == '[' || c == ']' || c == '\\') {
                    regex.append('\\');
                }
                regex.append(c);
            }
        }
        if (rawSpec.charAt(rawSpec.length() - 1) == '/') {
            regex.append('/');
        }
        regex.append('$');
        this.pattern = Pattern.compile(regex.toString());
        int varcount = varNames.size();
        this.variables = varNames.toArray(new String[varcount]);
        String sig = String.valueOf(segmentSignature);
        this.group = Pattern.matches("^e*$", sig) ? PathSpecGroup.EXACT : (Pattern.matches("^e*v+", sig) ? PathSpecGroup.PREFIX_GLOB : (Pattern.matches("^v+e+", sig) ? PathSpecGroup.SUFFIX_GLOB : PathSpecGroup.MIDDLE_GLOB));
    }

    private void assertIsValidVariableLiteral(String variable) {
        boolean valid;
        int len = variable.length();
        int i = 0;
        boolean bl = valid = len > 0;
        while (valid && i < len) {
            int codepoint = variable.codePointAt(i);
            i += Character.charCount(codepoint);
            if (this.isValidBasicLiteralCodepoint(codepoint) || Character.isSupplementaryCodePoint(codepoint)) continue;
            if (codepoint == 37) {
                if (i + 2 > len) {
                    valid = false;
                    continue;
                }
                codepoint = TypeUtil.convertHexDigit(variable.codePointAt(i++)) << 4;
                if (this.isValidBasicLiteralCodepoint(codepoint |= TypeUtil.convertHexDigit(variable.codePointAt(i++)))) continue;
            }
            valid = false;
        }
        if (!valid) {
            StringBuilder err = new StringBuilder();
            err.append("Syntax Error: variable {");
            err.append(variable);
            err.append("} an invalid variable name: ");
            err.append(this.pathSpec);
            throw new IllegalArgumentException(err.toString());
        }
    }

    private boolean isValidBasicLiteralCodepoint(int codepoint) {
        if (codepoint >= 97 && codepoint <= 122 || codepoint >= 65 && codepoint <= 90 || codepoint >= 48 && codepoint <= 57) {
            return true;
        }
        if (VARIABLE_SYMBOLS.indexOf(codepoint) >= 0) {
            return true;
        }
        if (VARIABLE_RESERVED.indexOf(codepoint) >= 0) {
            LOG.warn("Detected URI Template reserved symbol [{}] in path spec \"{}\"", Character.valueOf((char)codepoint), this.pathSpec);
            return false;
        }
        return false;
    }

    public Map<String, String> getPathParams(String path) {
        Matcher matcher = this.getMatcher(path);
        if (matcher.matches()) {
            if (this.group == PathSpecGroup.EXACT) {
                return Collections.emptyMap();
            }
            HashMap<String, String> ret = new HashMap<String, String>();
            int groupCount = matcher.groupCount();
            for (int i = 1; i <= groupCount; ++i) {
                ret.put(this.variables[i - 1], matcher.group(i));
            }
            return ret;
        }
        return null;
    }

    public int getVariableCount() {
        return this.variables.length;
    }

    public String[] getVariables() {
        return this.variables;
    }

    static {
        FORBIDDEN_SEGMENTS.add("/./");
        FORBIDDEN_SEGMENTS.add("/../");
        FORBIDDEN_SEGMENTS.add("//");
    }
}

