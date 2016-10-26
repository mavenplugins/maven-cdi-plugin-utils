package com.itemis.maven.plugins.cdi.util;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ParallelWorkflowStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ProcessingWorkflow;
import com.itemis.maven.plugins.cdi.internal.util.workflow.SimpleWorkflowStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowUtil;

public class WorkflowUtilTest {

  @Test
  public void testParseWorkflow_Sequential() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf1_sequential"), "wf1");

    Assert.assertEquals("wf1", workflow.getGoal());
    for (WorkflowStep step : workflow.getProcessingSteps()) {
      Assert.assertFalse("The workflow contains at least one parallel step although all steps should be sequential!",
          step.isParallel());
      Assert.assertEquals("Workflow step of wrong type", SimpleWorkflowStep.class, step.getClass());
      SimpleWorkflowStep s = (SimpleWorkflowStep) step;
      Assert.assertNotNull(
          "The processing step id doesn't seem to be set correctly for the sequential processing step.",
          Strings.emptyToNull(s.getStepId()));
    }
  }

  @Test
  public void testParseWorkflow_Parallel() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf1_parallel"), "wf1");

    Assert.assertEquals("wf1", workflow.getGoal());
    // The first step is a parallel step
    WorkflowStep parallelStep = workflow.getProcessingSteps().get(0);
    Assert.assertTrue("The first processing step should be a parallel one.", parallelStep.isParallel());
    Assert.assertEquals("Workflow step of wrong type", ParallelWorkflowStep.class, parallelStep.getClass());
    ParallelWorkflowStep s = (ParallelWorkflowStep) parallelStep;
    Assert.assertFalse("The parallel step should contain a list of parallel step ids.", s.getSteps().isEmpty());

    for (int i = 1; i < workflow.getProcessingSteps().size(); i++) {
      WorkflowStep step = workflow.getProcessingSteps().get(i);
      Assert.assertFalse(
          "The workflow contains a further parallel steps although all but the first step should be sequential!",
          step.isParallel());
    }
  }

  @Test
  public void testParseWorkflow_Sequential_Qualifiers() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf2_sequential_qualifiers"), "wf2");

    Assert.assertEquals("wf2", workflow.getGoal());
    for (WorkflowStep step : workflow.getProcessingSteps()) {
      Assert.assertEquals("Workflow step of wrong type", SimpleWorkflowStep.class, step.getClass());
      SimpleWorkflowStep s = (SimpleWorkflowStep) step;
      Assert.assertNotNull(
          "The processing step id doesn't seem to be set correctly for the sequential processing step.",
          Strings.emptyToNull(s.getStepId()));
      Assert.assertTrue(
          "The processing step qualifier doesn't seem to be set correctly for the sequential processing step.",
          s.getQualifier().isPresent());
    }
  }

  @Test
  public void testParseWorkflow_Sequential_Data() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf1_sequential_data"), "wf1");

    Assert.assertEquals("wf1", workflow.getGoal());
    for (WorkflowStep step : workflow.getProcessingSteps()) {
      SimpleWorkflowStep s = (SimpleWorkflowStep) step;
      if ("check3".equals(s.getStepId())) {
        Assert.assertTrue(
            "No default execution data was parsed from the workflow for step with id '" + s.getStepId() + "'",
            s.getDefaultExecutionData().isPresent());
        Assert.assertTrue(
            "No default rollback data was parsed from the workflow for step with id '" + s.getStepId() + "'",
            s.getDefaultRollbackData().isPresent());
        Assert.assertEquals(
            "The parsed default execution data of step '" + s.getStepId() + "' differs from the expected result.",
            "xyz, abc", s.getDefaultExecutionData().get());
        Assert.assertEquals(
            "The parsed default rollback data of step '" + s.getStepId() + "' differs from the expected result.", "123",
            s.getDefaultRollbackData().get());
      }
    }
  }

  @Test
  public void testParseWorkflow_Parallel_Data() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("wf1_parallel_data"), "wf1");

    Assert.assertEquals("wf1", workflow.getGoal());
    // The first step is a parallel step
    WorkflowStep parallelStep = workflow.getProcessingSteps().get(0);
    Assert.assertTrue("The first processing step should be a parallel one.", parallelStep.isParallel());
    Assert.assertEquals("Workflow step of wrong type", ParallelWorkflowStep.class, parallelStep.getClass());
    ParallelWorkflowStep s = (ParallelWorkflowStep) parallelStep;
    Assert.assertFalse("The parallel step should contain a list of parallel step ids.", s.getSteps().isEmpty());

    for (int i = 1; i < workflow.getProcessingSteps().size(); i++) {
      SimpleWorkflowStep step = (SimpleWorkflowStep) workflow.getProcessingSteps().get(i);
      if ("check1".equals(step.getStepId())) {
        Assert.assertTrue(
            "No default execution data was parsed from the workflow for step with id '" + step.getStepId() + "'",
            step.getDefaultExecutionData().isPresent());
        Assert.assertFalse("Default rollback data was parsed from the workflow for step with id '" + step.getStepId()
            + "' but nothing was configured!", step.getDefaultRollbackData().isPresent());
        Assert.assertEquals(
            "The parsed default execution data of step '" + step.getStepId() + "' differs from the expected result.",
            "test", step.getDefaultExecutionData().get());
      } else if ("check2[y]".equals(step.getCompositeStepId())) {
        Assert.assertTrue(
            "No default execution data was parsed from the workflow for step with id '" + step.getStepId() + "'",
            step.getDefaultExecutionData().isPresent());
        Assert.assertTrue(
            "No default rollback data was parsed from the workflow for step with id '" + step.getStepId() + "'",
            step.getDefaultRollbackData().isPresent());
        Assert.assertEquals(
            "The parsed default execution data of step '" + step.getStepId() + "' differs from the expected result.",
            "xyz, test=>123", step.getDefaultExecutionData().get());
        Assert.assertEquals(
            "The parsed default rollback data of step '" + step.getStepId() + "' differs from the expected result.",
            "a=>1   ,b=>2", step.getDefaultRollbackData().get());
      }
    }
  }

  private InputStream getWorkflowAsStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream("workflows/" + name);
  }
}
