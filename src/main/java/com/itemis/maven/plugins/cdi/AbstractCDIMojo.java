package com.itemis.maven.plugins.cdi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.itemis.maven.plugins.cdi.annotations.MojoProduces;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.beans.CdiBeanWrapper;
import com.itemis.maven.plugins.cdi.beans.CdiProducerBean;
import com.itemis.maven.plugins.cdi.util.CDIUtil;
import com.itemis.maven.plugins.cdi.util.MavenUtil;
import com.itemis.maven.plugins.cdi.util.WorkflowExecutor;
import com.itemis.maven.plugins.cdi.util.WorkflowUtil;
import com.itemis.maven.plugins.cdi.workflow.ProcessingWorkflow;

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
 * private List&lt;MavenProject&gt; reactorProjects;
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
 * @since 1.0.0
 */
public class AbstractCDIMojo extends AbstractMojo implements Extension {
  private static final String DEFAULT_WORKFLOW_DIR = "META-INF/workflows";

  @Component
  private ArtifactResolver resolver;

  @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repoSystemSession;

  @Parameter(readonly = true, defaultValue = "${project.remotePluginRepositories}")
  private List<RemoteRepository> pluginRepos;

  @Parameter
  private File workflowDescriptor;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Weld weld = new Weld();
    weld.addExtension(this);
    addPluginDependencies(weld);
    WeldContainer weldContainer = null;
    try {
      weldContainer = weld.initialize();
      // IDEA: alternatively let the Mojo implement a method to return a custom workflow (for dynamic workflow
      // composition)
      ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowDescriptor(), getGoalName());
      Map<String, CDIMojoProcessingStep> steps = getAllProcessingSteps(weldContainer);

      WorkflowExecutor executor = new WorkflowExecutor(workflow, steps, getLog());
      executor.validate();
      executor.execute();
    } finally {
      if (weldContainer != null && weldContainer.isRunning()) {
        weldContainer.shutdown();
      }
    }
  }

  @SuppressWarnings("unused")
  // will be called automatically by the CDI container once the bean discovery has finished
  private void processMojoCdiProducerFields(@Observes AfterBeanDiscovery event, BeanManager beanManager)
      throws MojoExecutionException {
    for (Field f : getClass().getDeclaredFields()) {
      if (f.isAnnotationPresent(MojoProduces.class)) {
        try {
          f.setAccessible(true);
          event.addBean(
              new CdiBeanWrapper<Object>(f.get(this), f.getGenericType(), f.getType(), CDIUtil.getCdiQualifiers(f)));
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
              CDIUtil.getCdiQualifiers(m)));
        } catch (Throwable t) {
          throw new MojoExecutionException("Could not process CDI producer method of the Mojo.", t);
        }
      }
    }
  }

  private void addPluginDependencies(Weld weld) throws MojoExecutionException {
    PluginDescriptor pluginDescriptor = getPluginDescriptor();
    List<Dependency> dependencies = pluginDescriptor.getPlugin().getDependencies();
    for (Dependency d : dependencies) {
      Optional<File> f = MavenUtil.resolvePluginDependency(d, this.pluginRepos, this.resolver, this.repoSystemSession);
      if (f.isPresent()) {
        CDIUtil.addAllClasses(weld, getClass().getClassLoader(), f.get(), getLog());
      }
    }
  }

  private Map<String, CDIMojoProcessingStep> getAllProcessingSteps(WeldContainer weldContainer) {
    Map<String, CDIMojoProcessingStep> steps = Maps.newHashMap();
    Collection<CDIMojoProcessingStep> beans = CDIUtil.getAllBeansOfType(weldContainer, CDIMojoProcessingStep.class);
    for (CDIMojoProcessingStep bean : beans) {
      ProcessingStep annotation = bean.getClass().getAnnotation(ProcessingStep.class);
      if (annotation != null) {
        String id = annotation.id();
        Preconditions.checkState(!steps.containsKey(id), "The processing step id '" + id + "' is not unique!");
        steps.put(id, bean);
      }
    }
    return steps;
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

  private PluginDescriptor getPluginDescriptor() {
    return (PluginDescriptor) getPluginContext().get("pluginDescriptor");
  }

  private InputStream getWorkflowDescriptor() throws MojoExecutionException {
    if (this.workflowDescriptor != null && this.workflowDescriptor.exists() && this.workflowDescriptor.isFile()) {
      try {
        return new FileInputStream(this.workflowDescriptor);
      } catch (Exception e) {
        throw new MojoExecutionException("Unable to load custom workflow for goal " + getGoalName(), e);
      }
    }
    return Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(DEFAULT_WORKFLOW_DIR + "/" + getGoalName());
  }
}
