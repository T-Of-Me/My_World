/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Pattern;

public abstract class PatternMatcher {
    public abstract void matched(URI var1) throws Exception;

    public void match(Pattern pattern, URI[] uris, boolean isNullInclusive) throws Exception {
        if (uris != null) {
            String[] patterns = pattern == null ? null : pattern.pattern().split(",");
            ArrayList<Pattern> subPatterns = new ArrayList<Pattern>();
            for (int i = 0; patterns != null && i < patterns.length; ++i) {
                subPatterns.add(Pattern.compile(patterns[i]));
            }
            if (subPatterns.isEmpty()) {
                subPatterns.add(pattern);
            }
            if (subPatterns.isEmpty()) {
                this.matchPatterns(null, uris, isNullInclusive);
            } else {
                for (Pattern p : subPatterns) {
                    this.matchPatterns(p, uris, isNullInclusive);
                }
            }
        }
    }

    public void matchPatterns(Pattern pattern, URI[] uris, boolean isNullInclusive) throws Exception {
        for (int i = 0; i < uris.length; ++i) {
            URI uri = uris[i];
            String s = uri.toString();
            if ((pattern != null || !isNullInclusive) && (pattern == null || !pattern.matcher(s).matches())) continue;
            this.matched(uris[i]);
        }
    }
}

