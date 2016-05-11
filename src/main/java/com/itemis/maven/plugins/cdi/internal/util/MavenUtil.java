package com.itemis.maven.plugins.cdi.internal.util;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Optional;

/**
 * A utility class for maven related stuff such as resolving of dependencies, ...
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public class MavenUtil {
  /**
   * Uses the aether to resolve a plugin dependency and returns the file for further processing.
   *
   * @param d the dependency to resolve.
   * @param pluginRepos the plugin repositories to use for dependency resolution.
   * @param resolver the resolver for aether access.
   * @param repoSystemSession the session for the resolver.
   * @return optionally a file which is the resolved dependency.
   */
  public static Optional<File> resolvePluginDependency(Dependency d, List<RemoteRepository> pluginRepos,
      ArtifactResolver resolver, RepositorySystemSession repoSystemSession) {
    Artifact a = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(a);
    artifactRequest.setRepositories(pluginRepos);
    try {
      ArtifactResult artifactResult = resolver.resolveArtifact(repoSystemSession, artifactRequest);
      if (artifactResult.getArtifact() != null) {
        return Optional.fromNullable(artifactResult.getArtifact().getFile());
      }
      return Optional.absent();
    } catch (ArtifactResolutionException e) {
      return Optional.absent();
    }
  }
}
