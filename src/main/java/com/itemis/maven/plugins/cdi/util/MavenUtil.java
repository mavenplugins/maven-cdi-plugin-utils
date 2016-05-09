package com.itemis.maven.plugins.cdi.util;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.google.common.base.Optional;

public class MavenUtil {
  public static Optional<File> resolvePluginDependency(Dependency d, List<RemoteRepository> pluginRepos,
      ArtifactResolver resolver, RepositorySystemSession repoSystemSession) throws MojoExecutionException {
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
      throw new MojoExecutionException("Could not resolve plugin dependency (" + a + ")", e);
    }
  }
}
