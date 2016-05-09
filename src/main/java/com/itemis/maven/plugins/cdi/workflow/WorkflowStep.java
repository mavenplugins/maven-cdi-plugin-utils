package com.itemis.maven.plugins.cdi.workflow;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class WorkflowStep {
  private boolean parallel;
  protected Set<String> stepIds;

  protected WorkflowStep(boolean parallel) {
    this.parallel = parallel;
    this.stepIds = Sets.newHashSet();
  }

  public boolean isParallel() {
    return this.parallel;
  }

  public String getStepId() {
    return Iterables.getOnlyElement(this.stepIds);
  }

  public Set<String> getParallelStepIds() {
    return Collections.unmodifiableSet(this.stepIds);
  }

  public static Builder sequencial() {
    return new Builder(false);
  }

  public static Builder parallel() {
    return new Builder(true);
  }

  public static class Builder {
    private WorkflowStep step;
    private boolean parallel;

    private Builder(boolean parallel) {
      this.parallel = parallel;
      this.step = new WorkflowStep(parallel);
    }

    public Builder setSequencialStep(String stepId) {
      Preconditions.checkState(!this.parallel,
          "Building a parallel workflow step does not allow setting a sequencial step id.");
      Preconditions.checkState(this.step.stepIds.isEmpty(),
          "The sequencial workflow step allows only the addition of one processing step.");
      this.step.stepIds.add(stepId);
      return this;
    }

    public Builder addParallelSteps(String... stepIds) {
      Preconditions.checkState(this.parallel,
          "Building a sequencial workflow step does not allow the addition of a parallel step ids.");
      for (String id : stepIds) {
        this.step.stepIds.add(id);
      }
      return this;
    }

    public WorkflowStep build() {
      Preconditions.checkState(!this.step.stepIds.isEmpty(),
          "Workflow step setup is not yet finished, there are no processing step ids set!");
      return this.step;
    }
  }
}
