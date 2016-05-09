package com.itemis.maven.plugins.cdi.util;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.itemis.maven.plugins.cdi.workflow.ProcessingWorkflow;
import com.itemis.maven.plugins.cdi.workflow.WorkflowStep;

public class WorkflowUtilTest {

  @Test
  public void testParseWorkflow_Sequencial() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf1_sequencial"), "wf1");

    Assert.assertEquals("wf1", workflow.getGoal());
    for (WorkflowStep step : workflow.getProcessingSteps()) {
      Assert.assertFalse("The workflow contains at leas one parallel step although all steps should be sequencial!",
          step.isParallel());
      Assert.assertNotNull(
          "The processing step id doesn't seem to be set correctly for the sequencial processing step.",
          Strings.emptyToNull(step.getStepId()));
    }
  }

  @Test
  public void testParseWorkflow_Parallel() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf1_parallel"), "wf1");

    Assert.assertEquals("wf1", workflow.getGoal());
    // The first step is a parallel step
    WorkflowStep parallelStep = workflow.getProcessingSteps().get(0);
    Assert.assertTrue("The first processing step should be a parallel one.", parallelStep.isParallel());
    Assert.assertFalse("The parallel step should contain a list of parallel step ids.",
        parallelStep.getParallelStepIds().isEmpty());

    for (int i = 1; i < workflow.getProcessingSteps().size(); i++) {
      WorkflowStep step = workflow.getProcessingSteps().get(i);
      Assert.assertFalse(
          "The workflow contains a further parallel steps although all but the first step should be sequencial!",
          step.isParallel());
    }
  }

  private InputStream getWorkflowAsStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream("workflows/" + name);
  }
}
