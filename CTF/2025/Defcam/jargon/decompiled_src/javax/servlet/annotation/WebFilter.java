/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebInitParam;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface WebFilter {
    public String description() default "";

    public String displayName() default "";

    public WebInitParam[] initParams() default {};

    public String filterName() default "";

    public String smallIcon() default "";

    public String largeIcon() default "";

    public String[] servletNames() default {};

    public String[] value() default {};

    public String[] urlPatterns() default {};

    public DispatcherType[] dispatcherTypes() default {DispatcherType.REQUEST};

    public boolean asyncSupported() default false;
}

