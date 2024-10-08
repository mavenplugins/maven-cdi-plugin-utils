package com.itemis.maven.plugins.cdi.util;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.itemis.maven.plugins.cdi.CDIMojoProcessingStep;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ParallelWorkflowStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ProcessingWorkflow;
import com.itemis.maven.plugins.cdi.internal.util.workflow.SimpleWorkflowStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowUtil;

public class WorkflowUtilTest {

  @Test
  public void testParseWorkflow_Sequential() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("sequential"), "wf1");

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
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("parallel"), "wf2");

    Assert.assertEquals("wf2", workflow.getGoal());
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
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("sequential_qualifiers"), "wf3");

    Assert.assertEquals("wf3", workflow.getGoal());
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
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("sequential_data"), "wf4");

    Assert.assertEquals("wf4", workflow.getGoal());
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
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("parallel_data"), "wf5");

    Assert.assertEquals("wf5", workflow.getGoal());
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

  @Test
  public void testParseWorkflow_TryFinally() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("try-finally"), "wf6");

    Assert.assertEquals("wf6", workflow.getGoal());
    List<WorkflowStep> trySteps = workflow.getProcessingSteps();
    Assert.assertEquals("Expected the standard workflow to have exactly 3 processing steps.", 3, trySteps.size());
    Assert.assertEquals("step1[1]", ((SimpleWorkflowStep) trySteps.get(0)).getCompositeStepId());
    Assert.assertEquals("step2", ((SimpleWorkflowStep) trySteps.get(1)).getCompositeStepId());
    Assert.assertEquals("step1[2]", ((SimpleWorkflowStep) trySteps.get(2)).getCompositeStepId());

    List<SimpleWorkflowStep> finallySteps = workflow.getFinallySteps();
    Assert.assertEquals("Expected the finally workflow to have exactly 2 processing steps.", 2, finallySteps.size());
    Assert.assertEquals("step1[3]", finallySteps.get(0).getCompositeStepId());
    Assert.assertEquals("step3", finallySteps.get(1).getCompositeStepId());
  }

  @Test
  public void testParseWorkflow_TryFinally_Complex() {
    ProcessingWorkflow workflow = WorkflowUtil.parseWorkflow(getWorkflowAsStream("try-finally_complex"), "wf7");

    Assert.assertEquals("wf7", workflow.getGoal());
    List<WorkflowStep> trySteps = workflow.getProcessingSteps();
    Assert.assertEquals("Expected the standard workflow to have exactly 2 processing steps.", 2, trySteps.size());

    Assert.assertFalse("Expected a sequential step as the first one.", trySteps.get(0).isParallel());
    SimpleWorkflowStep step1 = (SimpleWorkflowStep) trySteps.get(0);
    Assert.assertEquals("step1[1]", step1.getCompositeStepId());
    Assert.assertTrue("Step 1 should have default execution data assigned.",
        step1.getDefaultExecutionData().isPresent());
    Assert.assertEquals("echo test", step1.getDefaultExecutionData().get());
    Assert.assertTrue("Step 1 should have default rollback data assigned.", step1.getDefaultRollbackData().isPresent());
    Assert.assertEquals("echo 123", step1.getDefaultRollbackData().get());

    Assert.assertTrue("Expected a parallel step as the second one.", trySteps.get(1).isParallel());
    ParallelWorkflowStep step2 = (ParallelWorkflowStep) trySteps.get(1);
    Set<SimpleWorkflowStep> parallelSteps = step2.getSteps();
    Assert.assertEquals("Expected two steps to be executed in parallel.", 2, parallelSteps.size());

    for (SimpleWorkflowStep step : parallelSteps) {
      Assert.assertTrue("step2".equals(step.getCompositeStepId()) || "step1[2]".equals(step.getCompositeStepId()));
      if ("step2".equals(step.getCompositeStepId())) {
        Assert.assertTrue("The first parallel step should have default execution data assigned.",
            step.getDefaultExecutionData().isPresent());
        Assert.assertEquals("abc", step.getDefaultExecutionData().get());
        Assert.assertFalse("The first parallel step should not have default rollback data assigned.",
            step.getDefaultRollbackData().isPresent());
      } else {
        Assert.assertFalse("The second parallel step should not have default execution data assigned.",
            step.getDefaultExecutionData().isPresent());
        Assert.assertFalse("The second parallel step should not have default rollback data assigned.",
            step.getDefaultRollbackData().isPresent());
      }
    }

    List<SimpleWorkflowStep> finallySteps = workflow.getFinallySteps();
    Assert.assertEquals("Expected the finally workflow to have exactly 2 processing steps.", 2, finallySteps.size());

    SimpleWorkflowStep fStep1 = finallySteps.get(0);
    Assert.assertEquals("step1[3]", fStep1.getCompositeStepId());
    Assert.assertFalse("The first finally step should not have default execution data assigned.",
        fStep1.getDefaultExecutionData().isPresent());
    Assert.assertTrue("The first finally step should have default rollback data assigned.",
        fStep1.getDefaultRollbackData().isPresent());

    SimpleWorkflowStep fStep2 = finallySteps.get(1);
    Assert.assertEquals("step3", fStep2.getCompositeStepId());
    Assert.assertFalse("The first finally step should not have default execution data assigned.",
        fStep2.getDefaultExecutionData().isPresent());
    Assert.assertFalse("The first finally step should not have default rollback data assigned.",
        fStep2.getDefaultRollbackData().isPresent());
  }

  private InputStream getWorkflowAsStream(String name) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream("workflows/" + name);
  }

  @Test
  public void testRenderAvailableSteps() {
    final Map<String, ProcessingStep> mapProcessingSteps = Maps.newHashMap();
    ProcessingStep annotation = TestExecHook.class.getAnnotation(ProcessingStep.class);
    mapProcessingSteps.put(annotation.id(), annotation);
    annotation = TestHttpRequestHook.class.getAnnotation(ProcessingStep.class);
    mapProcessingSteps.put(annotation.id(), annotation);
    annotation = TestMavenHook.class.getAnnotation(ProcessingStep.class);
    mapProcessingSteps.put(annotation.id(), annotation);
    final String expected = "" // nl
        + "┌─────────────┬──────────────────────────────────────────────────┬──────────┐" + SystemUtils.LINE_SEPARATOR
        + "│     ID      │                   DESCRIPTION                    │ REQUIRES │" + SystemUtils.LINE_SEPARATOR
        + "│             │                                                  │  ONLINE  │" + SystemUtils.LINE_SEPARATOR
        + "╞═════════════╪══════════════════════════════════════════════════╪══════════╡" + SystemUtils.LINE_SEPARATOR
        + "│ exec        │ Executes shell commands such as shell or batch   │   true   │" + SystemUtils.LINE_SEPARATOR
        + "│             │ script execution.                                │          │" + SystemUtils.LINE_SEPARATOR
        + "├─────────────┼──────────────────────────────────────────────────┼──────────┤" + SystemUtils.LINE_SEPARATOR
        + "│ httpRequest │ Send HTTP requests such as POST, PUT or GET as   │   true   │" + SystemUtils.LINE_SEPARATOR
        + "│             │ part of your processing logic.                   │          │" + SystemUtils.LINE_SEPARATOR
        + "├─────────────┼──────────────────────────────────────────────────┼──────────┤" + SystemUtils.LINE_SEPARATOR
        + "│ mvn         │ Invoke a separate Maven build process during     │   true   │" + SystemUtils.LINE_SEPARATOR
        + "│             │ your processing logic.                           │          │" + SystemUtils.LINE_SEPARATOR
        + "└─────────────┴──────────────────────────────────────────────────┴──────────┘";
    Assert.assertEquals(expected, WorkflowUtil.renderAvailableSteps(mapProcessingSteps));
  }

  /**
   * Inner classes for tests of {@link WorkflowUtil#renderAvailableSteps(java.util.Map)}.
   */
  @ProcessingStep(id = "exec", description = "Executes shell commands such as shell or batch script execution.")
  private static class TestExecHook extends ANOPProcessingStep {
  }

  @ProcessingStep(id = "httpRequest", description = "Send HTTP requests such as POST, PUT or GET as part of your processing logic.", requiresOnline = true)
  private static class TestHttpRequestHook extends ANOPProcessingStep {
  }

  @ProcessingStep(id = "mvn", description = "Invoke a separate Maven build process during your processing logic.")
  private class TestMavenHook extends ANOPProcessingStep {
  }

  private static abstract class ANOPProcessingStep implements CDIMojoProcessingStep {

    @Override
    public void execute(ExecutionContext context) throws MojoExecutionException, MojoFailureException {
      // nothing to do
    }

  }
}
