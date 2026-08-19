/**
 * Shared kernel: base entity types, value objects, and cross-cutting
 * exception handling used by every other module.
 *
 * <p>Declared OPEN so it is exempt from Spring Modulith's normal
 * "only the module's root package is public API" rule — every other
 * module may depend on it freely, and it must never depend back on them.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package zm.agriswift.common;