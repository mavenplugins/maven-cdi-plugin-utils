package com.itemis.maven.plugins.cdi.annotations;

/**
 * The goal mappings used in conjunction with the {@link ProcessingStep} annotation.<br>
 * This annotation can be used to specify goal name to execution order mappings.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public @interface Goal {
  /**
   * @return the name of the goal for which this step shall be executed.
   */
  String name();

  /**
   * @return the step number from which the order will be calculated.
   */
  int stepNumber();

  /**
   * @return whether this step will be executed for the specified goal or not.
   */
  boolean enabled() default true;
}
