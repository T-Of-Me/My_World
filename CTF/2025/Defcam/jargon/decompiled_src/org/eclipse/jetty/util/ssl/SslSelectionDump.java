/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.ssl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;

public class SslSelectionDump
extends ContainerLifeCycle
implements Dumpable {
    private final String type;
    private CaptionedList enabled = new CaptionedList("Enabled");
    private CaptionedList disabled = new CaptionedList("Disabled");

    public SslSelectionDump(String type, String[] supportedByJVM, String[] enabledByJVM, String[] excludedByConfig, String[] includedByConfig) {
        this.type = type;
        this.addBean(this.enabled);
        this.addBean(this.disabled);
        List<String> jvmEnabled = Arrays.asList(enabledByJVM);
        List excludedPatterns = Arrays.stream(excludedByConfig).map(entry -> Pattern.compile(entry)).collect(Collectors.toList());
        List includedPatterns = Arrays.stream(includedByConfig).map(entry -> Pattern.compile(entry)).collect(Collectors.toList());
        Arrays.stream(supportedByJVM).sorted(Comparator.naturalOrder()).forEach(entry -> {
            boolean isPresent = true;
            StringBuilder s = new StringBuilder();
            s.append((String)entry);
            if (!jvmEnabled.contains(entry)) {
                if (isPresent) {
                    s.append(" -");
                    isPresent = false;
                }
                s.append(" JreDisabled:java.security");
            }
            for (Pattern pattern : excludedPatterns) {
                Matcher m = pattern.matcher((CharSequence)entry);
                if (!m.matches()) continue;
                if (isPresent) {
                    s.append(" -");
                    isPresent = false;
                } else {
                    s.append(",");
                }
                s.append(" ConfigExcluded:'").append(pattern.pattern()).append('\'');
            }
            if (!includedPatterns.isEmpty()) {
                boolean isIncluded = false;
                for (Pattern pattern : includedPatterns) {
                    Matcher m = pattern.matcher((CharSequence)entry);
                    if (!m.matches()) continue;
                    isIncluded = true;
                    break;
                }
                if (!isIncluded) {
                    if (isPresent) {
                        s.append(" -");
                        isPresent = false;
                    } else {
                        s.append(",");
                    }
                    s.append(" ConfigIncluded:NotSpecified");
                }
            }
            if (isPresent) {
                this.enabled.add(s.toString());
            } else {
                this.disabled.add(s.toString());
            }
        });
    }

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        this.dumpBeans(out, indent, new Collection[0]);
    }

    @Override
    protected void dumpThis(Appendable out) throws IOException {
        out.append(this.type).append(" Selections").append(System.lineSeparator());
    }

    private static class CaptionedList
    extends ArrayList<String>
    implements Dumpable {
        private final String caption;

        public CaptionedList(String caption) {
            this.caption = caption;
        }

        @Override
        public String dump() {
            return ContainerLifeCycle.dump(this);
        }

        @Override
        public void dump(Appendable out, String indent) throws IOException {
            out.append(this.caption);
            out.append(" (size=").append(Integer.toString(this.size())).append(")");
            out.append(System.lineSeparator());
            ContainerLifeCycle.dump(out, indent, this);
        }
    }
}

