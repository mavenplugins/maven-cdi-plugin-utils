package de.itemis.maven.plugins.cdi;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Qualifier;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import de.itemis.maven.plugins.cdi.annotations.MojoExecution;
import de.itemis.maven.plugins.cdi.annotations.MojoProduces;

/**
 * An abstract Mojo that enabled CDI-based dependency injection for the current maven plugin.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public class AbstractCDIMojo extends AbstractMojo implements Extension {
  private static final String FILE_EXTENSION_CLASS = "class";

  @Component
  private ArtifactResolver resolver;

  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repoSystemSession;

  @Parameter(readonly = true, defaultValue = "${project.remotePluginRepositories}")
  private List<RemoteRepository> pluginRepos;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Weld weld = new Weld();
    weld.addExtension(this);
    addPluginDependenciesToWeld(weld);
    WeldContainer weldContainer = weld.initialize();

    Multimap<Integer, InjectableCdiMojo> mojos = getMojos(weldContainer);
    executeMojos(mojos);

    weldContainer.shutdown();
  }

  private void addPluginDependenciesToWeld(Weld weld) throws MojoExecutionException {
    PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().get("pluginDescriptor");
    List<Dependency> dependencies = pluginDescriptor.getPlugin().getDependencies();
    for (Dependency d : dependencies) {
      Optional<File> f = resolvePluginDependency(d);
      if (f.isPresent()) {
        Set<String> classNames = null;
        if (f.get().isFile() && f.get().getAbsolutePath().endsWith(".jar")) {
          try {
            JarFile jarFile = new JarFile(f.get());
            classNames = getAllClassNames(jarFile);
          } catch (IOException e) {
            throw new MojoExecutionException(
                "Resolved plugin dependency could not be loaded as a JAR file (" + f.get().getAbsolutePath() + ")", e);
          }
        } else if (f.get().isDirectory()) {
          classNames = getAllClassNames(f.get());
        }

        for (String className : classNames) {
          try {
            Class<?> cls = getClass().getClassLoader().loadClass(className);
            weld.addBeanClass(cls);
          } catch (ClassNotFoundException e) {
            getLog().warn("Could not load the following class which might cause later issues: " + className);
            if (getLog().isDebugEnabled()) {
              getLog().debug(e);
            }
          }
        }
      }
    }
  }

  private Optional<File> resolvePluginDependency(Dependency d) throws MojoExecutionException {
    Artifact a = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(a);
    artifactRequest.setRepositories(this.pluginRepos);
    try {
      ArtifactResult artifactResult = this.resolver.resolveArtifact(this.repoSystemSession, artifactRequest);
      if (artifactResult.getArtifact() != null) {
        return Optional.fromNullable(artifactResult.getArtifact().getFile());
      }
      return Optional.absent();
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException("Could not resolve plugin dependency (" + a + ")", e);
    }
  }

  private Set<String> getAllClassNames(JarFile f) {
    Set<String> classNames = Sets.newHashSet();
    Enumeration<?> e = f.entries();
    while (e.hasMoreElements()) {
      JarEntry je = (JarEntry) e.nextElement();
      String extension = Files.getFileExtension(je.getName());
      if (Objects.equal(FILE_EXTENSION_CLASS, extension)) {
        String className = je.getName().substring(0, je.getName().length() - 6);
        className = className.replace('/', '.');
        classNames.add(className);
      }
    }
    return classNames;
  }

  private Set<String> getAllClassNames(File folder) {
    Set<String> classNames = Sets.newHashSet();
    for (File f : Files.fileTreeTraverser().preOrderTraversal(folder)) {
      String extension = Files.getFileExtension(f.getName());
      if (Objects.equal(FILE_EXTENSION_CLASS, extension)) {
        String basePath = f.getAbsolutePath().replace(folder.getAbsolutePath(), "");
        String className = basePath.substring(0, basePath.length() - 6);
        className = className.replace('/', '.').replace('\\', '.');
        if (className.startsWith(".")) {
          className = className.substring(1);
        }
        classNames.add(className);
      }
    }
    return classNames;
  }

  private void executeMojos(Multimap<Integer, InjectableCdiMojo> mojos)
      throws MojoExecutionException, MojoFailureException {
    List<Integer> keys = Lists.newArrayList(mojos.keySet());
    Collections.sort(keys);
    for (Integer key : keys) {
      for (InjectableCdiMojo mojo : mojos.get(key)) {
        mojo.execute();
      }
    }
  }

  private Multimap<Integer, InjectableCdiMojo> getMojos(WeldContainer weldContainer) {
    Multimap<Integer, InjectableCdiMojo> mojos = ArrayListMultimap.create();
    Set<Bean<?>> mojoBeans = weldContainer.getBeanManager().getBeans(InjectableCdiMojo.class, AnyLiteral.INSTANCE);
    for (Bean<?> b : mojoBeans) {
      @SuppressWarnings("unchecked")
      Bean<InjectableCdiMojo> bean = (Bean<InjectableCdiMojo>) b;
      CreationalContext<InjectableCdiMojo> creationalContext = weldContainer.getBeanManager()
          .createCreationalContext(bean);
      InjectableCdiMojo mojo = bean.create(creationalContext);
      int order = 0;
      boolean enabled = true;
      MojoExecution execution = mojo.getClass().getAnnotation(MojoExecution.class);
      if (execution != null) {
        order = execution.order();
        enabled = execution.enabled();
      }
      if (enabled) {
        mojos.put(order, mojo);
      }
    }
    return mojos;
  }

  @SuppressWarnings("unused")
  // will be called automatically by the CDI container once the bean discovery has finished
  private void processMojoCdiProducerFields(@Observes AfterBeanDiscovery event, BeanManager beanManager)
      throws MojoExecutionException {
    for (Field f : getClass().getDeclaredFields()) {
      if (f.isAnnotationPresent(MojoProduces.class)) {
        try {
          f.setAccessible(true);
          event.addBean(new CdiBeanWrapper<Object>(f.get(this), f.getGenericType(), f.getType(), getCdiQualifiers(f)));
        } catch (Throwable t) {
          throw new MojoExecutionException("Could not process CDI producer field of the Mojo.", t);
        }
      }
    }
  }

  @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
  // will be called automatically by the CDI container once the bean discovery has finished
  private void processMojoCdiProducerMethods(@Observes AfterBeanDiscovery event, BeanManager beanManager)
      throws MojoExecutionException {
    for (Method m : getClass().getDeclaredMethods()) {
      if (m.getReturnType() != Void.class && m.isAnnotationPresent(MojoProduces.class)) {
        try {
          event.addBean(new CdiProducerBean(m, this, beanManager, m.getGenericReturnType(), m.getReturnType(),
              getCdiQualifiers(m)));
        } catch (Throwable t) {
          throw new MojoExecutionException("Could not process CDI producer method of the Mojo.", t);
        }
      }
    }
  }

  private Set<Annotation> getCdiQualifiers(AccessibleObject x) {
    Set<Annotation> qualifiers = Sets.newHashSet();
    for (Annotation annotation : x.getAnnotations()) {
      if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifiers.add(annotation);
      }
    }
    if (qualifiers.isEmpty()) {
      qualifiers.add(DefaultLiteral.INSTANCE);
    }
    return qualifiers;
  }
}
