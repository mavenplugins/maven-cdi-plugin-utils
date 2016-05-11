package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * A workflow representing the processing step order for a specific goal.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public class ProcessingWorkflow {
  private String goal;
  private List<WorkflowStep> steps;

  public ProcessingWorkflow(String goal) {
    this.goal = goal;
    this.steps = Lists.newArrayList();
  }

  public String getGoal() {
    return this.goal;
  }

  public void addProcessingStep(WorkflowStep step) {
    this.steps.add(step);
  }

  public List<WorkflowStep> getProcessingSteps() {
    return this.steps;
  }
}
