package de.itemis.maven.plugins.cdi.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import de.itemis.maven.plugins.cdi.InjectableCdiMojo;

/**
 * A class-level annotation used to specify some metadata for the executions of the {@link InjectableCdiMojo injected
 * CDI Mojo instances}.<br>
 * You can f.i. specify the execution order of the Mojos or disable some of them.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface MojoExecution {
  String name();

  int order();

  boolean enabled() default true;
}
