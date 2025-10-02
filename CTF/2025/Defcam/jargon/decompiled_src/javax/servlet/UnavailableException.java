/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

public class UnavailableException
extends ServletException {
    private Servlet servlet;
    private boolean permanent;
    private int seconds;

    public UnavailableException(Servlet servlet, String msg) {
        super(msg);
        this.servlet = servlet;
        this.permanent = true;
    }

    public UnavailableException(int seconds, Servlet servlet, String msg) {
        super(msg);
        this.servlet = servlet;
        this.seconds = seconds <= 0 ? -1 : seconds;
        this.permanent = false;
    }

    public UnavailableException(String msg) {
        super(msg);
        this.permanent = true;
    }

    public UnavailableException(String msg, int seconds) {
        super(msg);
        this.seconds = seconds <= 0 ? -1 : seconds;
        this.permanent = false;
    }

    public boolean isPermanent() {
        return this.permanent;
    }

    public Servlet getServlet() {
        return this.servlet;
    }

    public int getUnavailableSeconds() {
        return this.permanent ? -1 : this.seconds;
    }
}

