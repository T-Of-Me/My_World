/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import org.eclipse.jetty.util.Utf8Appendable;

public class Utf8StringBuilder
extends Utf8Appendable {
    final StringBuilder _buffer;

    public Utf8StringBuilder() {
        super(new StringBuilder());
        this._buffer = (StringBuilder)this._appendable;
    }

    public Utf8StringBuilder(int capacity) {
        super(new StringBuilder(capacity));
        this._buffer = (StringBuilder)this._appendable;
    }

    @Override
    public int length() {
        return this._buffer.length();
    }

    @Override
    public void reset() {
        super.reset();
        this._buffer.setLength(0);
    }

    public StringBuilder getStringBuilder() {
        this.checkState();
        return this._buffer;
    }

    public String toString() {
        this.checkState();
        return this._buffer.toString();
    }
}

