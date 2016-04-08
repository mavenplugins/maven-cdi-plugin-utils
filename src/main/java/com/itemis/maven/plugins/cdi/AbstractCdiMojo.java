package com.itemis.maven.plugins.cdi;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Stack;
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
import org.apache.maven.plugin.descriptor.MojoDescriptor;
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
import com.itemis.maven.plugins.cdi.annotations.MojoExecution;
import com.itemis.maven.plugins.cdi.annotations.MojoProduces;
import com.itemis.maven.plugins.cdi.annotations.RollbackOnError;
import com.itemis.maven.plugins.cdi.beans.CdiBeanWrapper;
import com.itemis.maven.plugins.cdi.beans.CdiProducerBean;

/**
 * An abstract Mojo that enabled CDI-based dependency injection for the current maven plugin.<br>
 * This Mojo enables you to decouple different parts of your plugin implementation and also dynamically inject
 * additional funktionality into your plugin. It provides the possibility to use nearly the full CDI stack as there are
 * injection, producers, interceptors, decorators, alternatives, ...<br>
 * <br>
 *
 * <b>ATTENTION:</b> Please do not use annotations such as {@code @javax.inject.Inject} or
 * {@code @javax.enterprise.inject.Produces} directly in your Mojo! There are special replacements for that in the
 * annotations package of this library. Using CDI annotations directly in the Mojo would trigger Maven's own CDI
 * adaption!<br>
 * <br>
 *
 * Using this abstract Mojo as the parent of your own Mojo, you can simply see the Mojo class as a data dispatcher
 * container whose single responsibility is to provide paramters for your business logic implementations. Simply get the
 * Mojo parameters injected and use the producer annotation to provide the bean to your implementations:
 *
 * <pre>
 * &#64;Parameter
 * &#64;MojoProduces
 * &#64;Named("sourcePath")
 * private String sourcePath;
 *
 * &#64;Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
 * &#64;MojoProduces
 * &#64;Named("reactorProjects")
 * private List<MavenProject> reactorProjects;
 * </pre>
 *
 * Or use a producer method for the logger:
 *
 * <pre>
 * &#64;MojoProduces
 * public MavenLogWrapper createLogWrapper() {
 *   MavenLogWrapper log = new MavenLogWrapper(getLog());
 *   if (this.enableLogTimestamps) {
 *     log.enableLogTimestamps();
 *   }
 *   return log;
 * }
 * </pre>
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 */
public class AbstractCdiMojo extends AbstractMojo implements Extension {
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
    WeldContainer weldContainer = null;
    try {
      weldContainer = weld.initialize();
      Multimap<Integer, InjectableCdiMojo> mojos = getMojos(weldContainer);
      executeMojos(mojos);
    } finally {
      if (weldContainer != null && weldContainer.isRunning()) {
        weldContainer.shutdown();
      }
    }
  }

  private void addPluginDependenciesToWeld(Weld weld) throws MojoExecutionException {
    PluginDescriptor pluginDescriptor = getPluginDescriptor();
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
    Stack<InjectableCdiMojo> executedMojos = new Stack<InjectableCdiMojo>();

    for (Integer key : keys) {
      for (InjectableCdiMojo mojo : mojos.get(key)) {
        try {
          executedMojos.push(mojo);
          mojo.execute();
        } catch (Throwable t) {
          rollbackMojos(executedMojos, t);
          // throw original exception after rollback!
          if (t instanceof MojoExecutionException) {
            throw (MojoExecutionException) t;
          } else if (t instanceof MojoFailureException) {
            throw (MojoFailureException) t;
          } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
          } else {
            throw new RuntimeException(t);
          }
        }
      }
    }
  }

  private void rollbackMojos(Stack<InjectableCdiMojo> executedMojos, Throwable t) {
    while (!executedMojos.empty()) {
      rollbackMojo(executedMojos.pop(), t);
    }
  }

  private void rollbackMojo(InjectableCdiMojo mojo, Throwable t) {
    // get rollback methods and sort alphabetically
    List<Method> rollbackMethods = getRollbackMethods(mojo, t.getClass());
    rollbackMethods.sort(new Comparator<Method>() {
      @Override
      public int compare(Method m1, Method m2) {
        return m1.getName().compareTo(m2.getName());
      }
    });

    // call rollback methods
    for (Method rollbackMethod : rollbackMethods) {
      rollbackMethod.setAccessible(true);
      try {
        if (rollbackMethod.getParameters().length == 1) {
          rollbackMethod.invoke(mojo, t);
        } else {
          rollbackMethod.invoke(mojo);
        }
      } catch (ReflectiveOperationException e) {
        getLog().error("Error calling rollback method of Mojo.", e);
      }
    }
  }

  private <T extends Throwable> List<Method> getRollbackMethods(InjectableCdiMojo mojo, Class<T> causeType) {
    List<Method> rollbackMethods = Lists.newArrayList();
    for (Method m : mojo.getClass().getDeclaredMethods()) {
      RollbackOnError rollbackAnnotation = m.getAnnotation(RollbackOnError.class);
      if (rollbackAnnotation != null) {
        boolean considerMethod = false;

        // consider method for inclusion if no error types are declared or if one of the declared error types is a
        // supertype of the caught exception
        Class<? extends Throwable>[] errorTypes = rollbackAnnotation.value();
        if (errorTypes.length == 0) {
          considerMethod = true;
        } else {
          for (Class<? extends Throwable> errorType : errorTypes) {
            if (errorType.isAssignableFrom(causeType)) {
              considerMethod = true;
              break;
            }
          }
        }

        // now check also the method parameters (0 or one exception type)
        if (considerMethod) {
          Class<?>[] parameterTypes = m.getParameterTypes();
          switch (parameterTypes.length) {
            case 0:
              rollbackMethods.add(m);
              break;
            case 1:
              if (parameterTypes[0].isAssignableFrom(causeType)) {
                rollbackMethods.add(m);
              }
              break;
            default:
              getLog().warn(
                  "Found rollback method with more than one parameters! Only zero or one parameter of type <T extends Throwable> is allowed!");
              break;
          }
        }
      }
    }

    return rollbackMethods;
  }

  private Multimap<Integer, InjectableCdiMojo> getMojos(WeldContainer weldContainer) {
    Multimap<Integer, InjectableCdiMojo> mojos = ArrayListMultimap.create();
    String goalName = getGoalName();

    Set<Bean<?>> mojoBeans = weldContainer.getBeanManager().getBeans(InjectableCdiMojo.class, AnyLiteral.INSTANCE);
    // searches all beans for beans that have the matching goal name, ...
    for (Bean<?> b : mojoBeans) {
      @SuppressWarnings("unchecked")
      Bean<InjectableCdiMojo> bean = (Bean<InjectableCdiMojo>) b;
      CreationalContext<InjectableCdiMojo> creationalContext = weldContainer.getBeanManager()
          .createCreationalContext(bean);
      InjectableCdiMojo mojo = bean.create(creationalContext);

      int order = 0;
      boolean enabled = false;
      String mojoName = null;
      MojoExecution execution = mojo.getClass().getAnnotation(MojoExecution.class);
      if (execution != null) {
        order = execution.order();
        enabled = execution.enabled();
        mojoName = execution.name();
      }
      if (enabled && Objects.equal(goalName, mojoName)) {
        mojos.put(order, mojo);
      }
    }
    return mojos;
  }

  private String getGoalName() {
    PluginDescriptor pluginDescriptor = getPluginDescriptor();
    for (MojoDescriptor mojoDescriptor : pluginDescriptor.getMojos()) {
      if (mojoDescriptor.getImplementation().equals(getClass().getName())) {
        return mojoDescriptor.getGoal();
      }
    }
    return null;
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
    // no method parameter injection possible at the moment since the container is not yet initialized at this point!
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

  private PluginDescriptor getPluginDescriptor() {
    return (PluginDescriptor) getPluginContext().get("pluginDescriptor");
  }
}
