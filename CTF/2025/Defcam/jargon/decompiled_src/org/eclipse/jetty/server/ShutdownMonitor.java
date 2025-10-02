/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.lang.invoke.LambdaMetafactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.component.Destroyable;
import org.eclipse.jetty.util.component.LifeCycle;

public class ShutdownMonitor {
    private final Set<LifeCycle> _lifeCycles = new LinkedHashSet<LifeCycle>();
    private boolean debug = System.getProperty("DEBUG") != null;
    private final String host = System.getProperty("STOP.HOST", "127.0.0.1");
    private int port = Integer.parseInt(System.getProperty("STOP.PORT", "-1"));
    private String key = System.getProperty("STOP.KEY", null);
    private boolean exitVm = true;
    private boolean alive;

    public static ShutdownMonitor getInstance() {
        return Holder.instance;
    }

    protected static void reset() {
        Holder.instance = new ShutdownMonitor();
    }

    public static void register(LifeCycle ... lifeCycles) {
        ShutdownMonitor.getInstance().addLifeCycles(lifeCycles);
    }

    public static void deregister(LifeCycle lifeCycle) {
        ShutdownMonitor.getInstance().removeLifeCycle(lifeCycle);
    }

    public static boolean isRegistered(LifeCycle lifeCycle) {
        return ShutdownMonitor.getInstance().containsLifeCycle(lifeCycle);
    }

    private ShutdownMonitor() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void addLifeCycles(LifeCycle ... lifeCycles) {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            this._lifeCycles.addAll(Arrays.asList(lifeCycles));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void removeLifeCycle(LifeCycle lifeCycle) {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            this._lifeCycles.remove(lifeCycle);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean containsLifeCycle(LifeCycle lifeCycle) {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            return this._lifeCycles.contains(lifeCycle);
        }
    }

    private void debug(String format, Object ... args) {
        if (this.debug) {
            System.err.printf("[ShutdownMonitor] " + format + "%n", args);
        }
    }

    private void debug(Throwable t) {
        if (this.debug) {
            t.printStackTrace(System.err);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getKey() {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            return this.key;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getPort() {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            return this.port;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isExitVm() {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            return this.exitVm;
        }
    }

    public void setDebug(boolean flag) {
        this.debug = flag;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setExitVm(boolean exitVm) {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            if (this.alive) {
                throw new IllegalStateException("ShutdownMonitor already started");
            }
            this.exitVm = exitVm;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setKey(String key) {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            if (this.alive) {
                throw new IllegalStateException("ShutdownMonitor already started");
            }
            this.key = key;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setPort(int port) {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            if (this.alive) {
                throw new IllegalStateException("ShutdownMonitor already started");
            }
            this.port = port;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void start() throws Exception {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            if (this.alive) {
                this.debug("Already started", new Object[0]);
                return;
            }
            ServerSocket serverSocket = this.listen();
            if (serverSocket != null) {
                this.alive = true;
                Thread thread = new Thread(new ShutdownMonitorRunnable(serverSocket));
                thread.setDaemon(true);
                thread.setName("ShutdownMonitor");
                thread.start();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void stop() {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            this.alive = false;
            this.notifyAll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void await() throws InterruptedException {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            while (this.alive) {
                this.wait();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected boolean isAlive() {
        ShutdownMonitor shutdownMonitor = this;
        synchronized (shutdownMonitor) {
            return this.alive;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ServerSocket listen() {
        ServerSocket serverSocket;
        int port = this.getPort();
        if (port < 0) {
            this.debug("Not enabled (port < 0): %d", port);
            return null;
        }
        String key = this.getKey();
        try {
            ServerSocket serverSocket2 = new ServerSocket();
            serverSocket2.setReuseAddress(true);
            serverSocket2.bind(new InetSocketAddress(InetAddress.getByName(this.host), port));
            if (port == 0) {
                port = serverSocket2.getLocalPort();
                System.out.printf("STOP.PORT=%d%n", port);
                this.setPort(port);
            }
            if (key == null) {
                key = Long.toString((long)(9.223372036854776E18 * Math.random() + (double)this.hashCode() + (double)System.currentTimeMillis()), 36);
                System.out.printf("STOP.KEY=%s%n", key);
                this.setKey(key);
            }
            serverSocket = serverSocket2;
        }
        catch (Throwable x) {
            ServerSocket serverSocket3;
            try {
                this.debug(x);
                System.err.println("Error binding ShutdownMonitor to port " + port + ": " + x.toString());
                serverSocket3 = null;
            }
            catch (Throwable throwable) {
                this.debug("STOP.PORT=%d", port);
                this.debug("STOP.KEY=%s", key);
                throw throwable;
            }
            this.debug("STOP.PORT=%d", port);
            this.debug("STOP.KEY=%s", key);
            return serverSocket3;
        }
        this.debug("STOP.PORT=%d", port);
        this.debug("STOP.KEY=%s", key);
        return serverSocket;
    }

    public String toString() {
        return String.format("%s[port=%d,alive=%b]", this.getClass().getName(), this.getPort(), this.isAlive());
    }

    static /* synthetic */ void access$200(ShutdownMonitor x0, String x1, Object[] x2) {
        x0.debug(x1, x2);
    }

    static /* synthetic */ void access$400(ShutdownMonitor x0) {
        x0.stop();
    }

    private class ShutdownMonitorRunnable
    implements Runnable {
        private final ServerSocket serverSocket;

        private ShutdownMonitorRunnable(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         * Unable to fully structure code
         */
        @Override
        public void run() {
            block35: {
                ShutdownMonitor.access$200(ShutdownMonitor.this, "Started", new Object[0]);
                try {
                    key = ShutdownMonitor.this.getKey();
                    block24: while (true) {
                        try {
                            while (true) {
                                block37: {
                                    block38: {
                                        block39: {
                                            block34: {
                                                block36: {
                                                    block32: {
                                                        block33: {
                                                            socket = this.serverSocket.accept();
                                                            var3_5 = null;
                                                            reader = new LineNumberReader(new InputStreamReader(socket.getInputStream()));
                                                            receivedKey = reader.readLine();
                                                            if (key.equals(receivedKey)) break block32;
                                                            ShutdownMonitor.access$200(ShutdownMonitor.this, "Ignoring command with incorrect key: %s", new Object[]{receivedKey});
                                                            if (socket == null) continue;
                                                            if (var3_5 == null) break block33;
                                                            try {
                                                                socket.close();
                                                                continue block24;
                                                            }
                                                            catch (Throwable var6_11) {
                                                                var3_5.addSuppressed(var6_11);
                                                                continue;
                                                            }
                                                        }
                                                        socket.close();
                                                        continue;
                                                    }
                                                    cmd = reader.readLine();
                                                    ShutdownMonitor.access$200(ShutdownMonitor.this, "command=%s", new Object[]{cmd});
                                                    out = socket.getOutputStream();
                                                    exitVm = ShutdownMonitor.this.isExitVm();
                                                    if (!"stop".equalsIgnoreCase(cmd)) ** GOTO lbl53
                                                    ShutdownMonitor.access$200(ShutdownMonitor.this, "Performing stop command", new Object[0]);
                                                    this.stopLifeCycles((Predicate<LifeCycle>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, isRegistered(org.eclipse.jetty.util.component.LifeCycle ), (Lorg/eclipse/jetty/util/component/LifeCycle;)Z)(), exitVm);
                                                    ShutdownMonitor.access$200(ShutdownMonitor.this, "Informing client that we are stopped", new Object[0]);
                                                    this.informClient(out, "Stopped\r\n");
                                                    if (exitVm) break block34;
                                                    if (socket == null) break block35;
                                                    if (var3_5 == null) break block36;
                                                    try {
                                                        socket.close();
                                                    }
                                                    catch (Throwable var9_14) {
                                                        var3_5.addSuppressed(var9_14);
                                                    }
                                                    break block35;
                                                }
                                                socket.close();
                                                break block35;
                                            }
                                            ShutdownMonitor.access$200(ShutdownMonitor.this, "Killing JVM", new Object[0]);
                                            System.exit(0);
                                            break block37;
lbl53:
                                            // 1 sources

                                            if (!"forcestop".equalsIgnoreCase(cmd)) ** GOTO lbl75
                                            ShutdownMonitor.access$200(ShutdownMonitor.this, "Performing forced stop command", new Object[0]);
                                            this.stopLifeCycles((Predicate<LifeCycle>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$run$0(org.eclipse.jetty.util.component.LifeCycle ), (Lorg/eclipse/jetty/util/component/LifeCycle;)Z)(), exitVm);
                                            ShutdownMonitor.access$200(ShutdownMonitor.this, "Informing client that we are stopped", new Object[0]);
                                            this.informClient(out, "Stopped\r\n");
                                            if (exitVm) break block38;
                                            if (socket == null) break block35;
                                            if (var3_5 == null) break block39;
                                            try {
                                                socket.close();
                                            }
                                            catch (Throwable var9_15) {
                                                var3_5.addSuppressed(var9_15);
                                            }
                                            break block35;
                                        }
                                        socket.close();
                                        break block35;
                                    }
                                    ShutdownMonitor.access$200(ShutdownMonitor.this, "Killing JVM", new Object[0]);
                                    System.exit(0);
                                    break block37;
lbl75:
                                    // 1 sources

                                    if ("stopexit".equalsIgnoreCase(cmd)) {
                                        ShutdownMonitor.access$200(ShutdownMonitor.this, "Performing stop and exit commands", new Object[0]);
                                        this.stopLifeCycles((Predicate<LifeCycle>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, isRegistered(org.eclipse.jetty.util.component.LifeCycle ), (Lorg/eclipse/jetty/util/component/LifeCycle;)Z)(), true);
                                        ShutdownMonitor.access$200(ShutdownMonitor.this, "Informing client that we are stopped", new Object[0]);
                                        this.informClient(out, "Stopped\r\n");
                                        ShutdownMonitor.access$200(ShutdownMonitor.this, "Killing JVM", new Object[0]);
                                        System.exit(0);
                                        break block37;
                                    }
                                    if ("exit".equalsIgnoreCase(cmd)) {
                                        ShutdownMonitor.access$200(ShutdownMonitor.this, "Killing JVM", new Object[0]);
                                        System.exit(0);
                                        break block37;
                                    }
                                    if (!"status".equalsIgnoreCase(cmd)) break block37;
                                    this.informClient(out, "OK\r\n");
                                }
                                if (socket == null) continue;
                                if (var3_5 != null) {
                                    try {
                                        socket.close();
                                        continue block24;
                                    }
                                    catch (Throwable var4_7) {
                                        var3_5.addSuppressed(var4_7);
                                        continue;
                                    }
                                }
                                socket.close();
                            }
                            catch (Throwable var4_8) {
                                try {
                                    var3_5 = var4_8;
                                    throw var4_8;
                                }
                                catch (Throwable var10_16) {
                                    if (socket != null) {
                                        if (var3_5 != null) {
                                            try {
                                                socket.close();
                                            }
                                            catch (Throwable var11_17) {
                                                var3_5.addSuppressed(var11_17);
                                            }
                                        } else {
                                            socket.close();
                                        }
                                    }
                                    throw var10_16;
                                }
                            }
                        }
                        catch (Throwable x) {
                            ShutdownMonitor.access$300(ShutdownMonitor.this, x);
                            continue;
                        }
                        break;
                    }
                }
                catch (Throwable x) {
                    ShutdownMonitor.access$300(ShutdownMonitor.this, x);
                }
                finally {
                    IO.close(this.serverSocket);
                    ShutdownMonitor.access$400(ShutdownMonitor.this);
                    ShutdownMonitor.access$200(ShutdownMonitor.this, "Stopped", new Object[0]);
                }
            }
        }

        private void informClient(OutputStream out, String message) throws IOException {
            out.write(message.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void stopLifeCycles(Predicate<LifeCycle> predicate, boolean destroy) {
            ArrayList lifeCycles = new ArrayList();
            ShutdownMonitorRunnable shutdownMonitorRunnable = this;
            synchronized (shutdownMonitorRunnable) {
                lifeCycles.addAll(ShutdownMonitor.this._lifeCycles);
            }
            for (LifeCycle l : lifeCycles) {
                try {
                    if (l.isStarted() && predicate.test(l)) {
                        l.stop();
                    }
                    if (!(l instanceof Destroyable) || !destroy) continue;
                    ((Destroyable)((Object)l)).destroy();
                }
                catch (Throwable x) {
                    ShutdownMonitor.this.debug(x);
                }
            }
        }

        private static /* synthetic */ boolean lambda$run$0(LifeCycle l) {
            return true;
        }
    }

    private static class Holder {
        static ShutdownMonitor instance = new ShutdownMonitor();

        private Holder() {
        }
    }
}

