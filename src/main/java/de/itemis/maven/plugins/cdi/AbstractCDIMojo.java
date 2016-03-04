package de.itemis.maven.plugins.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Qualifier;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import de.itemis.maven.plugins.cdi.annotations.MojoExecution;
import de.itemis.maven.plugins.cdi.annotations.MojoProduces;

public class AbstractCDIMojo extends AbstractMojo implements Extension {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Weld weld = new Weld();
    weld.addExtension(this);
    WeldContainer weldContainer = weld.initialize();

    Multimap<Integer, InjectableCdiMojo> mojos = getMojos(weldContainer);
    executeMojos(mojos);

    weldContainer.shutdown();
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
      Bean<InjectableCdiMojo> bean = (Bean<InjectableCdiMojo>) b;
      CreationalContext<InjectableCdiMojo> creationalContext = weldContainer.getBeanManager()
          .createCreationalContext(bean);
      InjectableCdiMojo mojo = bean.create(creationalContext);
      int order = 0;
      MojoExecution execution = mojo.getClass().getAnnotation(MojoExecution.class);
      if (execution != null) {
        order = execution.order();
      }
      mojos.put(order, mojo);
    }
    return mojos;
  }

  private void processMojoCdiProducerFields(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    for (Field f : getClass().getDeclaredFields()) {
      if (f.isAnnotationPresent(MojoProduces.class)) {
        try {
          f.setAccessible(true);
          event.addBean(new CdiBeanWrapper<Object>(f.get(this), f.getGenericType(), f.getType(), getCdiQualifiers(f)));
        } catch (Throwable t) {
          // FIXME handle and log!
          throw new RuntimeException(t);
        }
      }
    }
  }

  private void processMojoCdiProducerMethods(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    for (Method m : getClass().getDeclaredMethods()) {
      if (m.getReturnType() != Void.class && m.isAnnotationPresent(MojoProduces.class)) {
        try {
          event.addBean(new CdiProducerBean(m, this, beanManager, m.getGenericReturnType(), m.getReturnType(),
              getCdiQualifiers(m)));
        } catch (Throwable t) {
          // FIXME handle and log!
          throw new RuntimeException(t);
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
