package com.itemis.maven.plugins.cdi;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;

/**
 * Classes of this type will automatically be executed as the primary plugin code once the CDI container is set up.<br>
 * You can influence the execution order or other parameters using the {@link ProcessingStep} annotation.<br>
 * <br>
 *
 * <b>Example Mojo:</b>
 *
 * <pre>
 * &#64;ProcessingStep(&#64;Goal(name = "run", stepNumber = "1"))
 * public class TestCdiMojo implements InjectableCdiMojo {
 *   &#64;Inject
 *   &#64;Named("sourcePath")
 *   private String sourcePath;
 *
 *   public void execute() throws MojoExecutionException, MojoFailureException {
 *     System.out.println(this.sourcePath);
 *   }
 * }
 * </pre>
 *
 * You can also use multiple mappings for one processing step:
 *
 * <pre>
 * &#64;ProcessingStep({ &#64;Goal(name = "run", stepNumber = "1"), &#64;Goal(name = "run", stepNumber = "7"),
 *     &#64;Goal(name = "dryRun", stepNumber = "3") })
 * public class TestCdiMojo implements InjectableCdiMojo {
 *   &#64;Inject
 *   &#64;Named("sourcePath")
 *   private String sourcePath;
 *
 *   public void execute() throws MojoExecutionException, MojoFailureException {
 *     System.out.println(this.sourcePath);
 *   }
 * }
 * </pre>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 1.0.0
 */
public interface CDIMojoProcessingStep {

  /**
   * The primary execution method which will be called as soon as this Mojo is ready for execution.<br>
   * If the execution of this method fails with any kind of {@link Throwable}, the Mojo will automatically search for
   * all methods annotated with the {@link RollbackOnError}. If the specified throwable type matches and the method's
   * optional argument matches the caught Exception type, the rollback method will be executed. It is possible to
   * declare multiple rollback methods for a Processing Step.
   *
   * @throws MojoExecutionException if an unexpected execution exception occurred.
   * @throws MojoFailureException an expected exceptional case during the Mojo execution.
   */
  void execute() throws MojoExecutionException, MojoFailureException;
}
