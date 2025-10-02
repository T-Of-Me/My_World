/*
 * Decompiled with CFR 0.152.
 */
package javax.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.servlet.annotation.ServletSecurity;

@Documented
@Retention(value=RetentionPolicy.RUNTIME)
public @interface HttpConstraint {
    public ServletSecurity.EmptyRoleSemantic value() default ServletSecurity.EmptyRoleSemantic.PERMIT;

    public ServletSecurity.TransportGuarantee transportGuarantee() default ServletSecurity.TransportGuarantee.NONE;

    public String[] rolesAllowed() default {};
}

