/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class DOMLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.newDocumentBuilder();
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }
}

