package com.itemis.maven.plugins.cdi.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.enterprise.inject.Typed;

import org.apache.maven.plugin.logging.Log;

import com.google.common.base.Preconditions;

/**
 * A wrapper around the maven logger that offers some extended functionalities like checking the enablement of the
 * appropriate log level before logging.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 *
 */
@Typed({ Logger.class, Log.class })
public class MavenLogWrapper implements Logger {
  private static final DateFormat FORMAT_TIMESTAMP = new SimpleDateFormat("HH:mm:ss,SSS ");

  private Log log;
  private String context;
  private boolean timestampsEnabled;

  public MavenLogWrapper(Log log) {
    this.log = log;
  }

  @Override
  public void setContextClass(Class<?> contextClass) {
    Preconditions.checkNotNull(contextClass, "The context class for the logger must not be null!");
    this.context = contextClass.getSimpleName();
  }

  @Override
  public void unsetContext() {
    this.context = null;
  }

  public boolean hasContext() {
    return this.context != null;
  }

  @Override
  public void enableLogTimestamps() {
    this.timestampsEnabled = true;
  }

  @Override
  public void disableLogTimestamps() {
    this.timestampsEnabled = false;
  }

  @Override
  public boolean isTimestampedLoggingEnabled() {
    return this.timestampsEnabled;
  }

  @Override
  public boolean isDebugEnabled() {
    return this.log.isDebugEnabled();
  }

  @Override
  public void debug(CharSequence content) {
    if (this.log.isDebugEnabled()) {
      this.log.debug(wrapContent(content));
    }
  }

  @Override
  public void debug(CharSequence content, Throwable error) {
    if (this.log.isDebugEnabled()) {
      this.log.debug(wrapContent(content), error);
    }
  }

  @Override
  public void debug(Throwable error) {
    if (this.log.isDebugEnabled()) {
      this.log.debug(error);
    }
  }

  @Override
  public boolean isInfoEnabled() {
    return this.log.isInfoEnabled();
  }

  @Override
  public void info(CharSequence content) {
    if (this.log.isInfoEnabled()) {
      this.log.info(wrapContent(content));
    }
  }

  @Override
  public void info(CharSequence content, Throwable error) {
    if (this.log.isInfoEnabled()) {
      this.log.info(wrapContent(content), error);
    }
  }

  @Override
  public void info(Throwable error) {
    if (this.log.isInfoEnabled()) {
      this.log.info(error);
    }
  }

  @Override
  public boolean isWarnEnabled() {
    return this.log.isWarnEnabled();
  }

  @Override
  public void warn(CharSequence content) {
    if (this.log.isWarnEnabled()) {
      this.log.warn(wrapContent(content));
    }
  }

  @Override
  public void warn(CharSequence content, Throwable error) {
    if (this.log.isWarnEnabled()) {
      this.log.warn(wrapContent(content), error);
    }
  }

  @Override
  public void warn(Throwable error) {
    if (this.log.isWarnEnabled()) {
      this.log.warn(error);
    }
  }

  @Override
  public boolean isErrorEnabled() {
    return this.log.isErrorEnabled();
  }

  @Override
  public void error(CharSequence content) {
    if (this.log.isErrorEnabled()) {
      this.log.error(wrapContent(content));
    }
  }

  @Override
  public void error(CharSequence content, Throwable error) {
    if (this.log.isErrorEnabled()) {
      this.log.error(wrapContent(content), error);
    }
  }

  @Override
  public void error(Throwable error) {
    if (this.log.isErrorEnabled()) {
      this.log.error(error);
    }
  }

  private CharSequence wrapContent(CharSequence content) {
    StringBuilder sb = new StringBuilder(content);
    wrapContentWithContext(sb);
    wrapContentWithTimestamp(sb);
    return sb;
  }

  private void wrapContentWithContext(StringBuilder content) {
    if (this.context != null) {
      content.insert(0, "[" + this.context + "] ");
    }
  }

  private void wrapContentWithTimestamp(StringBuilder content) {
    if (this.timestampsEnabled) {
      String date = FORMAT_TIMESTAMP.format(new Date());
      content.insert(0, date + " ");
    }
  }
}
