package com.itemis.maven.plugins.cdi.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;

/**
 * A class-level annotation used to specify some metadata for the executions of the {@link CDIMojoProcessingStep
 * injected CDI Mojo instances}.<br>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ProcessingStep {
  /**
   * @return the id of the processing step which is used for the orchestration of the processing workflow.
   * @since 2.0.0
   */
  String id();

  /**
   * @return a description of this processing step's responsibilities. This description will later be used as output to
   *         users of your plugin that want to provide a custom processing workflow.
   * @since 2.0.0
   */
  String description() default "";
}
