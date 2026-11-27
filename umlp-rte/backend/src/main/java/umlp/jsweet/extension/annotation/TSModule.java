/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.annotation;

import java.lang.annotation.*;

/**
 * Tells from which TypeScript module (e.g. an npm package such as {@code @umlp/basic})
 * the annotated type is imported in the generated TypeScript code.
 * <p>
 * On a package (in its {@code package-info.java}), it applies to all types of the
 * package and of its subpackages, unless a type or a nearer package declares its own.
 * Types within the annotated package (and its subpackages) do not import each other
 * from the module, as they belong to the module themselves.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Documented
public @interface TSModule {
  /**
   * The module to import from, e.g. {@code @umlp/basic}.
   */
  String value();
}
