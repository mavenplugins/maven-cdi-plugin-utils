package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * A representation of a processing step of the workflow. A step can be a single sequential processing step but can also
 * unite multiple steps that shall be executed in parallel.<br>
 * <br>
 * Please use the two methods {@link #sequential()} and {@link #parallel} to create a respective {@link Builder} for the
 * processing step setup.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
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

  /**
   * @return a builder for building a sequential processing step. This builder will not allow to configure parallel step
   *         executions.
   */
  public static Builder sequential() {
    return new Builder(false);
  }

  /**
   * @return a builder for building a parallel processing step. This builder will not allow to configure a sequential
   *         step execution.
   */
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

    public Builder setSequentialStep(String stepId) {
      Preconditions.checkState(!this.parallel,
          "Building a parallel workflow step does not allow setting a sequential step id.");
      Preconditions.checkState(this.step.stepIds.isEmpty(),
          "The sequential workflow step allows only the addition of one processing step.");
      this.step.stepIds.add(stepId);
      return this;
    }

    public Builder addParallelSteps(String... stepIds) {
      Preconditions.checkState(this.parallel,
          "Building a sequential workflow step does not allow the addition of a parallel step ids.");
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
