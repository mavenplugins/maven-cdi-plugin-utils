package de.itemis.maven.plugins.cdi.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The CDI injection annotation for use within the Mojo. Currently only usable for parameter injection in producer
 * methods<br>
 * <b>Note</b> that it is not possible to use {@code @javax.inject.Inject} directly in your Mojo since this would
 * trigger Maven's own pseudo CDI implementation. Within all other beans that are managed by CDI you can use
 * {@code @Inject} as usual.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Retention(RUNTIME)
@Target({ METHOD })
public @interface MojoInject {
}
