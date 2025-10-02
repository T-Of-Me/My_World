/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.preventers;

import javax.imageio.ImageIO;
import org.eclipse.jetty.util.preventers.AbstractLeakPreventer;

public class AppContextLeakPreventer
extends AbstractLeakPreventer {
    @Override
    public void prevent(ClassLoader loader) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Pinning classloader for AppContext.getContext() with " + loader, new Object[0]);
        }
        ImageIO.getUseCache();
    }
}

