/*
 * Decompiled with CFR 0.152.
 */
package org.sqlite;

public interface SQLiteCommitListener {
    public void onCommit();

    public void onRollback();
}

