package de.itemis.maven.plugins.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Qualifier;

import org.jboss.weld.literal.DefaultLiteral;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class CdiProducerBean<T> implements Bean<T> {
  private Method method;
  private Object hostInstance;
  private BeanManager beanManager;
  private Set<Annotation> qualifiers;
  private Type type;
  private Class<?> instanceClass;

  public CdiProducerBean(Method method, Object hostInstance, BeanManager beanManager, Type type, Class<?> instanceClass,
      Set<Annotation> qualifiers) {
    this.method = method;
    this.hostInstance = hostInstance;
    this.beanManager = beanManager;
    this.type = type;
    this.instanceClass = instanceClass;
    this.qualifiers = qualifiers;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T create(CreationalContext<T> creationalContext) {
    Object[] params = new Object[0];
    java.lang.reflect.Parameter[] parameters = this.method.getParameters();
    params = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      java.lang.reflect.Parameter p = parameters[i];
      Set<Annotation> qualifiers = getCdiQualifiers(p);

      Set<Bean<?>> beans = this.beanManager.getBeans(p.getType(),
          qualifiers.toArray(new Annotation[qualifiers.size()]));
      if (beans.size() == 1) {
        Bean<?> bean = Iterables.get(beans, 0);
        Object reference = this.beanManager.getReference(bean, p.getType(),
            this.beanManager.createCreationalContext(bean));
        params[i] = reference;
      } else {
        // FIXME handle -> ambiguous results
      }
    }

    T instance = null;
    try {
      this.method.setAccessible(true);
      instance = (T) this.method.invoke(this.hostInstance, params);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return instance;
  }

  @Override
  public void destroy(T instance, CreationalContext<T> creationalContext) {
  }

  @Override
  public Set<Type> getTypes() {
    return Collections.singleton(this.type);
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return this.qualifiers;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return Dependent.class;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Collections.emptySet();
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public Class<?> getBeanClass() {
    return this.instanceClass;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public boolean isNullable() {
    return true;
  }

  private Set<Annotation> getCdiQualifiers(java.lang.reflect.Parameter p) {
    Set<Annotation> qualifiers = Sets.newHashSet();
    for (Annotation annotation : p.getAnnotations()) {
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
