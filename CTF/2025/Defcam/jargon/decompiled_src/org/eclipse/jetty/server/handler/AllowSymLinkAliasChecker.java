/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

public class AllowSymLinkAliasChecker
implements ContextHandler.AliasCheck {
    private static final Logger LOG = Log.getLogger(AllowSymLinkAliasChecker.class);

    @Override
    public boolean check(String uri, Resource resource) {
        if (!(resource instanceof PathResource)) {
            return false;
        }
        PathResource pathResource = (PathResource)resource;
        try {
            Path path = pathResource.getPath();
            Path alias = pathResource.getAliasPath();
            if (path.equals(alias)) {
                return false;
            }
            if (this.hasSymbolicLink(path) && Files.isSameFile(path, alias)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Allow symlink {} --> {}", resource, pathResource.getAliasPath());
                }
                return true;
            }
        }
        catch (Exception e) {
            LOG.ignore(e);
        }
        return false;
    }

    private boolean hasSymbolicLink(Path path) {
        if (Files.isSymbolicLink(path)) {
            return true;
        }
        Path base = path.getRoot();
        for (Path segment : path) {
            if (!Files.isSymbolicLink(base = base.resolve(segment))) continue;
            return true;
        }
        return false;
    }
}

