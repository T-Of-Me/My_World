/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ArrayTernaryTrie;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject(value="Context Handler Collection")
public class ContextHandlerCollection
extends HandlerCollection {
    private static final Logger LOG = Log.getLogger(ContextHandlerCollection.class);
    private final ConcurrentMap<ContextHandler, Handler> _contextBranches = new ConcurrentHashMap<ContextHandler, Handler>();
    private volatile Trie<Map.Entry<String, Branch[]>> _pathBranches;
    private Class<? extends ContextHandler> _contextClass = ContextHandler.class;

    public ContextHandlerCollection() {
        super(true, new Handler[0]);
    }

    public ContextHandlerCollection(ContextHandler ... contexts) {
        super(true, (Handler[])contexts);
    }

    @ManagedOperation(value="update the mapping of context path to context")
    public void mapContexts() {
        ArrayTernaryTrie<Map.Entry<String, Branch[]>> arrayTernaryTrie;
        this._contextBranches.clear();
        if (this.getHandlers() == null) {
            this._pathBranches = new ArrayTernaryTrie<Map.Entry<String, Branch[]>>(false, 16);
            return;
        }
        HashMap<String, Branch[]> map = new HashMap<String, Branch[]>();
        for (Handler handler : this.getHandlers()) {
            Branch branch = new Branch(handler);
            for (String contextPath : branch.getContextPaths()) {
                Branch[] branches = (Branch[])map.get(contextPath);
                map.put(contextPath, ArrayUtil.addToArray(branches, branch, Branch.class));
            }
            for (ContextHandler contextHandler : branch.getContextHandlers()) {
                this._contextBranches.putIfAbsent(contextHandler, branch.getHandler());
            }
        }
        for (Map.Entry entry : map.entrySet()) {
            Branch[] branches = (Branch[])entry.getValue();
            Branch[] sorted = new Branch[branches.length];
            int i = 0;
            for (Branch branch : branches) {
                if (!branch.hasVirtualHost()) continue;
                sorted[i++] = branch;
            }
            for (Branch branch : branches) {
                if (branch.hasVirtualHost()) continue;
                sorted[i++] = branch;
            }
            entry.setValue(sorted);
        }
        int capacity = 512;
        block6: while (true) {
            arrayTernaryTrie = new ArrayTernaryTrie<Map.Entry<String, Branch[]>>(false, capacity);
            for (Map.Entry entry : map.entrySet()) {
                if (arrayTernaryTrie.put(((String)entry.getKey()).substring(1), entry)) continue;
                capacity += 512;
                continue block6;
            }
            break;
        }
        if (LOG.isDebugEnabled()) {
            for (String ctx : arrayTernaryTrie.keySet()) {
                LOG.debug("{}->{}", ctx, Arrays.asList((Object[])((Map.Entry)arrayTernaryTrie.get(ctx)).getValue()));
            }
        }
        this._pathBranches = arrayTernaryTrie;
    }

    @Override
    public void setHandlers(Handler[] handlers) {
        super.setHandlers(handlers);
        if (this.isStarted()) {
            this.mapContexts();
        }
    }

    @Override
    protected void doStart() throws Exception {
        this.mapContexts();
        super.doStart();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ContextHandler context;
        Handler[] handlers = this.getHandlers();
        if (handlers == null || handlers.length == 0) {
            return;
        }
        HttpChannelState async = baseRequest.getHttpChannelState();
        if (async.isAsync() && (context = async.getContextHandler()) != null) {
            Handler branch = (Handler)this._contextBranches.get(context);
            if (branch == null) {
                context.handle(target, baseRequest, request, response);
            } else {
                branch.handle(target, baseRequest, request, response);
            }
            return;
        }
        if (target.startsWith("/")) {
            Map.Entry<String, Branch[]> branches;
            int limit = target.length() - 1;
            while (limit >= 0 && (branches = this._pathBranches.getBest(target, 1, limit)) != null) {
                int l = branches.getKey().length();
                if (l == 1 || target.length() == l || target.charAt(l) == '/') {
                    for (Branch branch : branches.getValue()) {
                        branch.getHandler().handle(target, baseRequest, request, response);
                        if (!baseRequest.isHandled()) continue;
                        return;
                    }
                }
                limit = l - 2;
            }
        } else {
            for (int i = 0; i < handlers.length; ++i) {
                handlers[i].handle(target, baseRequest, request, response);
                if (!baseRequest.isHandled()) continue;
                return;
            }
        }
    }

    public ContextHandler addContext(String contextPath, String resourceBase) {
        try {
            ContextHandler context = this._contextClass.newInstance();
            context.setContextPath(contextPath);
            context.setResourceBase(resourceBase);
            this.addHandler(context);
            return context;
        }
        catch (Exception e) {
            LOG.debug(e);
            throw new Error(e);
        }
    }

    public Class<?> getContextClass() {
        return this._contextClass;
    }

    public void setContextClass(Class<? extends ContextHandler> contextClass) {
        if (contextClass == null || !ContextHandler.class.isAssignableFrom(contextClass)) {
            throw new IllegalArgumentException();
        }
        this._contextClass = contextClass;
    }

    private static final class Branch {
        private final Handler _handler;
        private final ContextHandler[] _contexts;

        Branch(Handler handler) {
            this._handler = handler;
            if (handler instanceof ContextHandler) {
                this._contexts = new ContextHandler[]{(ContextHandler)handler};
            } else if (handler instanceof HandlerContainer) {
                Handler[] contexts = ((HandlerContainer)((Object)handler)).getChildHandlersByClass(ContextHandler.class);
                this._contexts = new ContextHandler[contexts.length];
                System.arraycopy(contexts, 0, this._contexts, 0, contexts.length);
            } else {
                this._contexts = new ContextHandler[0];
            }
        }

        Set<String> getContextPaths() {
            HashSet<String> set = new HashSet<String>();
            for (ContextHandler context : this._contexts) {
                set.add(context.getContextPath());
            }
            return set;
        }

        boolean hasVirtualHost() {
            for (ContextHandler context : this._contexts) {
                if (context.getVirtualHosts() == null || context.getVirtualHosts().length <= 0) continue;
                return true;
            }
            return false;
        }

        ContextHandler[] getContextHandlers() {
            return this._contexts;
        }

        Handler getHandler() {
            return this._handler;
        }

        public String toString() {
            return String.format("{%s,%s}", this._handler, Arrays.asList(this._contexts));
        }
    }
}

