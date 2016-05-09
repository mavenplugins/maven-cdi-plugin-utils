package com.itemis.maven.plugins.cdi.workflow;

import java.util.List;

import com.google.common.collect.Lists;

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
