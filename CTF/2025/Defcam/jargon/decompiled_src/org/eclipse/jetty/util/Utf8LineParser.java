/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.nio.ByteBuffer;
import org.eclipse.jetty.util.Utf8StringBuilder;

public class Utf8LineParser {
    private State state = State.START;
    private Utf8StringBuilder utf;

    public String parse(ByteBuffer buf) {
        while (buf.remaining() > 0) {
            byte b = buf.get();
            if (!this.parseByte(b)) continue;
            this.state = State.START;
            return this.utf.toString();
        }
        return null;
    }

    private boolean parseByte(byte b) {
        switch (this.state) {
            case START: {
                this.utf = new Utf8StringBuilder();
                this.state = State.PARSE;
                return this.parseByte(b);
            }
            case PARSE: {
                if (this.utf.isUtf8SequenceComplete() && (b == 13 || b == 10)) {
                    this.state = State.END;
                    return this.parseByte(b);
                }
                this.utf.append(b);
                break;
            }
            case END: {
                if (b != 10) break;
                this.state = State.START;
                return true;
            }
        }
        return false;
    }

    private static enum State {
        START,
        PARSE,
        END;

    }
}

