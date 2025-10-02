/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;

@Inherited
@Documented
@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface ServletSecurity {
    public HttpConstraint value() default @HttpConstraint;

    public HttpMethodConstraint[] httpMethodConstraints() default {};

    public static enum TransportGuarantee {
        NONE,
        CONFIDENTIAL;

    }

    public static enum EmptyRoleSemantic {
        PERMIT,
        DENY;

    }
}

