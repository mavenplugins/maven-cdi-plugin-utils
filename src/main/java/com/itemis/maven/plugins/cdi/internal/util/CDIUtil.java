package com.itemis.maven.plugins.cdi.internal.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Qualifier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

/**
 * A utility class for handling CDI-specific tasks such as getting all beans of a specific type or adding beans to the
 * bean manager, ...
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public class CDIUtil {
  private static final String FILE_EXTENSION_CLASS = "class";

  /**
   * @param x the object from which all qualifier annotations shall be searched out.
   * @return a set of all qualifiers the object's class is annotated with.
   */
  public static Set<Annotation> getCdiQualifiers(AccessibleObject x) {
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

  /**
   * Searches the container for all beans of a certain type without respecting qualifiers.
   *
   * @param weldContainer the container providing the beans.
   * @param type the type of the beans to search for.
   * @return a collection of all found beans of the specified type.
   */
  public static <T> Collection<T> getAllBeansOfType(WeldContainer weldContainer, Class<T> type) {
    Collection<T> beans = Lists.newArrayList();
    Set<Bean<?>> cdiBeans = weldContainer.getBeanManager().getBeans(type, AnyLiteral.INSTANCE);
    // searches all beans for beans that have the matching goal name, ...
    for (Bean<?> b : cdiBeans) {
      @SuppressWarnings("unchecked")
      Bean<T> b2 = (Bean<T>) b;
      CreationalContext<T> creationalContext = weldContainer.getBeanManager().createCreationalContext(b2);
      T bean = b2.create(creationalContext);
      beans.add(bean);
    }
    return beans;
  }

  /**
   * Queries the specified file container (folder or JAR file) for all class files and adds all found classes to the
   * weld container so that these classes are later injectable.
   *
   * @param weld the CDI container to add the classes to.
   * @param classLoader the class loader used to query and load classes from the file container.
   * @param container the file container where to search classes. The container can be a folder or a JAR file.
   * @param log the log for processing output.
   * @throws MojoExecutionException if it was not possible to query the file container.
   */
  public static void addAllClasses(Weld weld, ClassLoader classLoader, File container, Log log)
      throws MojoExecutionException {
    Set<String> classNames = null;
    if (container.isFile() && container.getAbsolutePath().endsWith(".jar")) {
      try {
        JarFile jarFile = new JarFile(container);
        classNames = getAllClassNames(jarFile);
      } catch (IOException e) {
        throw new MojoExecutionException("Could not load the following JAR file: " + container.getAbsolutePath(), e);
      }
    } else if (container.isDirectory()) {
      classNames = getAllClassNames(container);
    }

    for (String className : classNames) {
      try {
        Class<?> cls = classLoader.loadClass(className);
        weld.addBeanClass(cls);
      } catch (ClassNotFoundException e) {
        log.error("Could not load the following class which might cause later issues: " + className);
        if (log.isDebugEnabled()) {
          log.debug(e);
        }
      }
    }
  }

  private static Set<String> getAllClassNames(JarFile f) {
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

  private static Set<String> getAllClassNames(File folder) {
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
}
