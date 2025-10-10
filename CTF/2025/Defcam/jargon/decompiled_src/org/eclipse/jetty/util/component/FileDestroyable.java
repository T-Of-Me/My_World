/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.component.Destroyable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

public class FileDestroyable
implements Destroyable {
    private static final Logger LOG = Log.getLogger(FileDestroyable.class);
    final List<File> _files = new ArrayList<File>();

    public FileDestroyable() {
    }

    public FileDestroyable(String file) throws IOException {
        this._files.add(Resource.newResource(file).getFile());
    }

    public FileDestroyable(File file) {
        this._files.add(file);
    }

    public void addFile(String file) throws IOException {
        try (Resource r = Resource.newResource(file);){
            this._files.add(r.getFile());
        }
    }

    public void addFile(File file) {
        this._files.add(file);
    }

    public void addFiles(Collection<File> files) {
        this._files.addAll(files);
    }

    public void removeFile(String file) throws IOException {
        try (Resource r = Resource.newResource(file);){
            this._files.remove(r.getFile());
        }
    }

    public void removeFile(File file) {
        this._files.remove(file);
    }

    @Override
    public void destroy() {
        for (File file : this._files) {
            if (!file.exists()) continue;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Destroy {}", file);
            }
            IO.delete(file);
        }
    }
}

