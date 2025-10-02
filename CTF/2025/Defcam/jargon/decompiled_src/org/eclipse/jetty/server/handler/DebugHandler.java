/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.DateCache;
import org.eclipse.jetty.util.RolloverFileOutputStream;

public class DebugHandler
extends HandlerWrapper
implements Connection.Listener {
    private DateCache _date = new DateCache("HH:mm:ss", Locale.US);
    private OutputStream _out;
    private PrintStream _print;

    /*
     * Loose catch block
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Response base_response = baseRequest.getResponse();
        Thread thread = Thread.currentThread();
        String old_name = thread.getName();
        boolean suspend = false;
        boolean retry = false;
        String name = (String)request.getAttribute("org.eclipse.jetty.thread.name");
        if (name == null) {
            name = old_name + ":" + baseRequest.getHttpURI();
        } else {
            retry = true;
        }
        String ex = null;
        try {
            if (retry) {
                this.print(name, "RESUME");
            } else {
                this.print(name, "REQUEST " + baseRequest.getRemoteAddr() + " " + request.getMethod() + " " + baseRequest.getHeader("Cookie") + "; " + baseRequest.getHeader("User-Agent"));
            }
            thread.setName(name);
            this.getHandler().handle(target, baseRequest, request, response);
        }
        catch (IOException ioe) {
            try {
                ex = ioe.toString();
                throw ioe;
                catch (ServletException se) {
                    ex = se.toString() + ":" + se.getCause();
                    throw se;
                }
                catch (RuntimeException rte) {
                    ex = rte.toString();
                    throw rte;
                }
                catch (Error e) {
                    ex = e.toString();
                    throw e;
                }
            }
            catch (Throwable throwable) {
                thread.setName(old_name);
                suspend = baseRequest.getHttpChannelState().isSuspended();
                if (suspend) {
                    request.setAttribute("org.eclipse.jetty.thread.name", name);
                    this.print(name, "SUSPEND");
                    throw throwable;
                }
                this.print(name, "RESPONSE " + base_response.getStatus() + (ex == null ? "" : "/" + ex) + " " + base_response.getContentType());
                throw throwable;
            }
        }
        thread.setName(old_name);
        suspend = baseRequest.getHttpChannelState().isSuspended();
        if (suspend) {
            request.setAttribute("org.eclipse.jetty.thread.name", name);
            this.print(name, "SUSPEND");
            return;
        }
        this.print(name, "RESPONSE " + base_response.getStatus() + (ex == null ? "" : "/" + ex) + " " + base_response.getContentType());
    }

    private void print(String name, String message) {
        long now = System.currentTimeMillis();
        String d = this._date.formatNow(now);
        int ms = (int)(now % 1000L);
        this._print.println(d + (ms > 99 ? "." : (ms > 9 ? ".0" : ".00")) + ms + ":" + name + " " + message);
    }

    @Override
    protected void doStart() throws Exception {
        if (this._out == null) {
            this._out = new RolloverFileOutputStream("./logs/yyyy_mm_dd.debug.log", true);
        }
        this._print = new PrintStream(this._out);
        for (Connector connector : this.getServer().getConnectors()) {
            if (!(connector instanceof AbstractConnector)) continue;
            ((AbstractConnector)connector).addBean((Object)this, false);
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        this._print.close();
        for (Connector connector : this.getServer().getConnectors()) {
            if (!(connector instanceof AbstractConnector)) continue;
            ((AbstractConnector)connector).removeBean(this);
        }
    }

    public OutputStream getOutputStream() {
        return this._out;
    }

    public void setOutputStream(OutputStream out) {
        this._out = out;
    }

    @Override
    public void onOpened(Connection connection) {
        this.print(Thread.currentThread().getName(), "OPENED " + connection.toString());
    }

    @Override
    public void onClosed(Connection connection) {
        this.print(Thread.currentThread().getName(), "CLOSED " + connection.toString());
    }
}

