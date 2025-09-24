package com.itemis.maven.plugins.cdi.util;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowUtil;
import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowValidator;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

@RunWith(DataProviderRunner.class)
public class WorkflowValidatorTest {

  @Test
  @DataProvider({ "try-finally", "try-finally_complex" })
  public void testValidate(String workflowName) throws MojoExecutionException {
    WorkflowValidator.validateSyntactically(getTrimmedWorkflowLines(workflowName));
  }

  @Test(expected = RuntimeException.class)
  @DataProvider({ "invalid/try-finally_noTryBlockOpening", "invalid/try-finally_noTryBlockClosing",
      "invalid/try-finally_noFinallyBlock" })
  public void testValidate_error(String workflowName) throws MojoExecutionException {
    WorkflowValidator.validateSyntactically(getTrimmedWorkflowLines(workflowName));
  }

  private List<String> getTrimmedWorkflowLines(String name) throws MojoExecutionException {
    return WorkflowUtil.getTrimmedWorkflowLines(WorkflowUtil.getResourceStream("workflows/" + name));
  }
}
