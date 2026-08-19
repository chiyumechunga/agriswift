package zm.agriswift.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces fine-grained permission checks on Controller endpoints or Service methods.
 *
 * Example usage:
 *  @RequirePermission("DEPOT_RECEIPT_CREATE")
 *  @RequirePermission(value = {"PAYOUT_APPROVE", "FINANCE_ADMIN"}, operator = Logical.OR)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * The permission(s) required to execute the method.
     */
    String[] value();

    /**
     * How to evaluate multiple permissions (defaults to requiring ALL).
     */
    Logical operator() default Logical.AND;

    enum Logical {
        AND, OR
    }
}