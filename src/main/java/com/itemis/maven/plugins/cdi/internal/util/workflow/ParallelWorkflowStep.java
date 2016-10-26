package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * A representation of a parallel processing step of the workflow.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.1.0
 */
public class ParallelWorkflowStep implements WorkflowStep {
  private Set<SimpleWorkflowStep> steps;

  private ParallelWorkflowStep() {
    this.steps = Sets.newHashSet();
  }

  @Override
  public boolean isParallel() {
    return true;
  }

  public Set<SimpleWorkflowStep> getSteps() {
    return Collections.unmodifiableSet(this.steps);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    ToStringHelper toStringHelper = MoreObjects.toStringHelper(this);
    toStringHelper.add("#steps", this.steps.size());
    int i = 1;
    for (SimpleWorkflowStep step : this.steps) {
      toStringHelper.add("step " + i++,
          step.getStepId() + (step.getQualifier().isPresent() ? "[" + step.getQualifier().get() + "]" : ""));
    }
    return toStringHelper.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.steps.toArray());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ParallelWorkflowStep) {
      ParallelWorkflowStep otherStep = (ParallelWorkflowStep) other;
      SetView<SimpleWorkflowStep> intersection = Sets.intersection(this.steps, otherStep.steps);
      return intersection.size() == this.steps.size();
    }
    return false;
  }

  public static class Builder {
    private ParallelWorkflowStep parallelStep;

    private Builder() {
      this.parallelStep = new ParallelWorkflowStep();
    }

    public Builder addSteps(SimpleWorkflowStep... steps) {
      for (SimpleWorkflowStep step : steps) {
        this.parallelStep.steps.add(step);
      }
      return this;
    }

    public ParallelWorkflowStep build() {
      return this.parallelStep;
    }
  }

  @Override
  public boolean containsId(String id) {
    for (SimpleWorkflowStep step : this.steps) {
      if (step.containsId(id)) {
        return true;
      }
    }
    return false;
  }
}
