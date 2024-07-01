package com.itemis.maven.plugins.cdi.exception;

import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowExecutor;

/**
 * An exception being handled by {@link WorkflowExecutor} to abort a workflow with rollback of previous actions but
 * ending normal with Maven success.
 *
 * @author mhoffrog
 */
public class EnforceRollbackWithoutErrorException extends RuntimeException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   *
   */
  public EnforceRollbackWithoutErrorException() {
  }

  /**
   * @param message the detail message
   */
  public EnforceRollbackWithoutErrorException(String message) {
    super(message);
  }

  /**
   * @param cause a {@link Throwable} causing this exception
   */
  public EnforceRollbackWithoutErrorException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message the detail message
   * @param cause   a {@link Throwable} causing this exception
   */
  public EnforceRollbackWithoutErrorException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param message            the detail message
   * @param cause              a {@link Throwable} causing this exception
   * @param enableSuppression  whether or not suppression is enabled
   * @param writableStackTrace whether or not the stack trace should
   *                             be writable
   */
  public EnforceRollbackWithoutErrorException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
