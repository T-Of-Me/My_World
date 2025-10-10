/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.descriptor;

import java.util.Collection;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

public interface JspConfigDescriptor {
    public Collection<TaglibDescriptor> getTaglibs();

    public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups();
}

