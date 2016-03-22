package com.itemis.maven.plugins.cdi;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.itemis.maven.plugins.cdi.annotations.MojoExecution;

/**
 * Classes of this type will automatically be executed as the primary plugin code once the CDI container is set up.<br>
 * You can influence the execution order or other parameters using the {@link MojoExecution} annotation.<br>
 * <br>
 *
 * <b>Example Mojo:</b>
 *
 * <pre>
 * &#64;MojoExecution(order = 0)
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
public interface InjectableCdiMojo {

  /**
   * The primary execution method which will be called as soon as this Mojo is ready for execution.<br>
   * If the execution of this method fails with any exception the method {@link #rollback()} will be called
   * automatically.
   *
   * @throws MojoExecutionException if an unexpected execution exception occurred.
   * @throws MojoFailureException an expected exceptional case during the Mojo execution.
   */
  void execute() throws MojoExecutionException, MojoFailureException;
}
