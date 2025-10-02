/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.IPAddressMap;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class IPAccessHandler
extends HandlerWrapper {
    private static final Logger LOG = Log.getLogger(IPAccessHandler.class);
    PathMap<IPAddressMap<Boolean>> _white = new PathMap(true);
    PathMap<IPAddressMap<Boolean>> _black = new PathMap(true);
    boolean _whiteListByPath = false;

    public IPAccessHandler() {
    }

    public IPAccessHandler(String[] white, String[] black) {
        if (white != null && white.length > 0) {
            this.setWhite(white);
        }
        if (black != null && black.length > 0) {
            this.setBlack(black);
        }
    }

    public void addWhite(String entry) {
        this.add(entry, this._white);
    }

    public void addBlack(String entry) {
        this.add(entry, this._black);
    }

    public void setWhite(String[] entries) {
        this.set(entries, this._white);
    }

    public void setBlack(String[] entries) {
        this.set(entries, this._black);
    }

    public void setWhiteListByPath(boolean whiteListByPath) {
        this._whiteListByPath = whiteListByPath;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        InetSocketAddress address;
        EndPoint endp;
        HttpChannel channel = baseRequest.getHttpChannel();
        if (channel != null && (endp = channel.getEndPoint()) != null && (address = endp.getRemoteAddress()) != null && !this.isAddrUriAllowed(address.getHostString(), baseRequest.getPathInfo())) {
            response.sendError(403);
            baseRequest.setHandled(true);
            return;
        }
        this.getHandler().handle(target, baseRequest, request, response);
    }

    protected void add(String entry, PathMap<IPAddressMap<Boolean>> patternMap) {
        if (entry != null && entry.length() > 0) {
            IPAddressMap<Boolean> addrMap;
            String path;
            int idx;
            boolean deprecated = false;
            if (entry.indexOf(124) > 0) {
                idx = entry.indexOf(124);
            } else {
                idx = entry.indexOf(47);
                deprecated = idx >= 0;
            }
            String addr = idx > 0 ? entry.substring(0, idx) : entry;
            String string = path = idx > 0 ? entry.substring(idx) : "/*";
            if (addr.endsWith(".")) {
                deprecated = true;
            }
            if (path != null && (path.startsWith("|") || path.startsWith("/*."))) {
                path = path.substring(1);
            }
            if ((addrMap = (IPAddressMap<Boolean>)patternMap.get(path)) == null) {
                addrMap = new IPAddressMap<Boolean>();
                patternMap.put(path, addrMap);
            }
            if (addr != null && !"".equals(addr)) {
                addrMap.put(addr, true);
            }
            if (deprecated) {
                LOG.debug(this.toString() + " - deprecated specification syntax: " + entry, new Object[0]);
            }
        }
    }

    protected void set(String[] entries, PathMap<IPAddressMap<Boolean>> patternMap) {
        patternMap.clear();
        if (entries != null && entries.length > 0) {
            for (String addrPath : entries) {
                this.add(addrPath, patternMap);
            }
        }
    }

    protected boolean isAddrUriAllowed(String addr, String path) {
        if (this._white.size() > 0) {
            boolean match = false;
            boolean matchedByPath = false;
            for (Map.Entry<String, IPAddressMap<Boolean>> entry : this._white.getMatches(path)) {
                matchedByPath = true;
                IPAddressMap<Boolean> addrMap = entry.getValue();
                if (addrMap == null || addrMap.size() != 0 && addrMap.match(addr) == null) continue;
                match = true;
                break;
            }
            if (this._whiteListByPath ? matchedByPath && !match : !match) {
                return false;
            }
        }
        if (this._black.size() > 0) {
            for (Map.Entry<String, IPAddressMap<Boolean>> entry : this._black.getMatches(path)) {
                IPAddressMap<Boolean> addrMap = entry.getValue();
                if (addrMap == null || addrMap.size() != 0 && addrMap.match(addr) == null) continue;
                return false;
            }
        }
        return true;
    }

    @Override
    public String dump() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.toString());
        buf.append(" WHITELIST:\n");
        this.dump(buf, this._white);
        buf.append(this.toString());
        buf.append(" BLACKLIST:\n");
        this.dump(buf, this._black);
        return buf.toString();
    }

    protected void dump(StringBuilder buf, PathMap<IPAddressMap<Boolean>> patternMap) {
        for (String path : patternMap.keySet()) {
            for (String addr : ((IPAddressMap)patternMap.get(path)).keySet()) {
                buf.append("# ");
                buf.append(addr);
                buf.append("|");
                buf.append(path);
                buf.append("\n");
            }
        }
    }
}

