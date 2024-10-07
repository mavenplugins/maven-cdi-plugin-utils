package com.itemis.maven.plugins.cdi.util;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowValidator;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

@RunWith(DataProviderRunner.class)
public class WorkflowValidatorTest {

  @Test
  @DataProvider({ "try-finally", "try-finally_complex" })
  public void testValidate(String workflowName) {
    WorkflowValidator.validateSyntactically(getWorkflowAsStream(workflowName));
  }

  @Test(expected = RuntimeException.class)
  @DataProvider({ "invalid/try-finally_noTryBlockOpening", "invalid/try-finally_noTryBlockClosing",
      "invalid/try-finally_noFinallyBlock" })
  public void testValidate_error(String workflowName) {
    WorkflowValidator.validateSyntactically(getWorkflowAsStream(workflowName));
  }

  private InputStream getWorkflowAsStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream("workflows/" + name);
  }
}
