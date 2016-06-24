package com.itemis.maven.plugins.cdi.logging;

import org.apache.maven.plugin.logging.Log;

public interface Logger extends Log {

  void setContextClass(Class<?> contextClass);

  void unsetContext();

  void enableLogTimestamps();

  void disableLogTimestamps();

  boolean isTimestampedLoggingEnabled();
}