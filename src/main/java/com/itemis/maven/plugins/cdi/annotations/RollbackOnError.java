package com.itemis.maven.plugins.cdi.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;

/**
 * This annotation can be put on any method of an {@link CDIMojoProcessingStep} with the following purpose and
 * restrictions:
 * <br>
 * <br>
 * <b>Purpose:</b> As soon as the execution of the Mojo instance has failed throwing an exception, the rollback
 * method(s)
 * are called automatically.<br>
 * <br>
 * <b>Restrictions:</b>
 * <ul>
 * <li>Any return type of the method will be ignored, method should be void!</li>
 * <li>Method may have exactly one parameter of type {@code <T extends Throwable>} which takes the cause for the
 * rollback.</li>
 * <li>Otherwise the signature must not declare any arguments</li>
 * <li>If the method signature declares a Throwable as the single argument but the type does not match the caught
 * exception, the method is skipped. If there are other rollback methods declared with matching exception types or
 * without one, these will be executed as usual.</li>
 * </ul>
 *
 * It is possible to declare several rollback methods! Each method with matching exception types will be executed in
 * ascending alphabetical order.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface RollbackOnError {
  /**
   * @return the error types for which this rollback method is triggered.
   */
  Class<? extends Throwable>[] value() default {};
}
