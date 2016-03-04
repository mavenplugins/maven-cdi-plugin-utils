package de.itemis.maven.plugins.cdi;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public interface InjectableCdiMojo {

  // IDEA maybe add method-level annotations for pre-execute, post-execute, rollback!
  void execute() throws MojoExecutionException, MojoFailureException;
}
