/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Documented
@Target(value={ElementType.TYPE})
public @interface ManagedObject {
    public String value() default "Not Specified";
}

