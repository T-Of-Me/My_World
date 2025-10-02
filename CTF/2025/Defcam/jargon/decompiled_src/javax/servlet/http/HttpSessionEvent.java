/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.http;

import java.util.EventObject;
import javax.servlet.http.HttpSession;

public class HttpSessionEvent
extends EventObject {
    private static final long serialVersionUID = -7622791603672342895L;

    public HttpSessionEvent(HttpSession source) {
        super(source);
    }

    public HttpSession getSession() {
        return (HttpSession)super.getSource();
    }
}

