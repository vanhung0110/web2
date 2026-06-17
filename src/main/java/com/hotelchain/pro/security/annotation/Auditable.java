package com.hotelchain.pro.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Auditable — annotation để tự động ghi audit log.
 * Dùng trên method Service hoặc Controller.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String action();
    String entityType() default "";
    boolean captureOldValue() default false;
}
