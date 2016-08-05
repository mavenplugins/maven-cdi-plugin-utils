package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.itemis.maven.plugins.cdi.ExecutionContext;

/**
 * A workflow representing the processing step order for a specific goal.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public class ProcessingWorkflow {
  private String goal;
  private List<WorkflowStep> steps;
  private Map<String, ExecutionContext> executionContexts;

  public ProcessingWorkflow(String goal) {
    this.goal = goal;
    this.steps = Lists.newArrayList();
    this.executionContexts = Maps.newHashMap();
  }

  public String getGoal() {
    return this.goal;
  }

  public void addProcessingStep(WorkflowStep step) {
    this.steps.add(step);
  }

  public void addExecutionContext(String stepId, ExecutionContext context) {
    this.executionContexts.put(stepId, context);
  }

  public List<WorkflowStep> getProcessingSteps() {
    return Collections.unmodifiableList(this.steps);
  }

  public ExecutionContext getExecutionContext(String stepId) {
    return this.executionContexts.get(stepId);
  }

  public boolean containsStep(String id) {
    for (WorkflowStep step : this.steps) {
      if (step.containsId(id)) {
        return true;
      }
    }
    return false;
  }
}
