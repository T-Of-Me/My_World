/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.servlet.listener;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ELContextCleaner
implements ServletContextListener {
    private static final Logger LOG = Log.getLogger(ELContextCleaner.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            Class beanELResolver = Loader.loadClass("javax.el.BeanELResolver");
            Field field = this.getField(beanELResolver);
            this.purgeEntries(field);
            if (LOG.isDebugEnabled()) {
                LOG.debug("javax.el.BeanELResolver purged", new Object[0]);
            }
        }
        catch (ClassNotFoundException beanELResolver) {
        }
        catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
            LOG.warn("Cannot purge classes from javax.el.BeanELResolver", e);
        }
        catch (NoSuchFieldException e) {
            LOG.debug("Not cleaning cached beans: no such field javax.el.BeanELResolver.properties", new Object[0]);
        }
    }

    protected Field getField(Class<?> beanELResolver) throws SecurityException, NoSuchFieldException {
        if (beanELResolver == null) {
            return null;
        }
        return beanELResolver.getDeclaredField("properties");
    }

    protected void purgeEntries(Field properties) throws IllegalArgumentException, IllegalAccessException {
        Map map;
        if (properties == null) {
            return;
        }
        if (!properties.isAccessible()) {
            properties.setAccessible(true);
        }
        if ((map = (Map)properties.get(null)) == null) {
            return;
        }
        Iterator itor = map.keySet().iterator();
        while (itor.hasNext()) {
            Class clazz = (Class)itor.next();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clazz: " + clazz + " loaded by " + clazz.getClassLoader(), new Object[0]);
            }
            if (Thread.currentThread().getContextClassLoader().equals(clazz.getClassLoader())) {
                itor.remove();
                if (!LOG.isDebugEnabled()) continue;
                LOG.debug("removed", new Object[0]);
                continue;
            }
            if (!LOG.isDebugEnabled()) continue;
            LOG.debug("not removed: contextclassloader=" + Thread.currentThread().getContextClassLoader() + "clazz's classloader=" + clazz.getClassLoader(), new Object[0]);
        }
    }
}

