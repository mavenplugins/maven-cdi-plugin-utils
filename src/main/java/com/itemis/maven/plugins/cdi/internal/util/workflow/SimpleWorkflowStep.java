package com.itemis.maven.plugins.cdi.internal.util.workflow;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * A representation of a sequential processing step of the workflow.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.1.0
 */
public class SimpleWorkflowStep implements WorkflowStep {
  String id;
  Optional<String> qualifier;

  public SimpleWorkflowStep(String id, Optional<String> qualifier) {
    this.id = id;
    this.qualifier = qualifier;
  }

  @Override
  public boolean isParallel() {
    return false;
  }

  public String getStepId() {
    return this.id;
  }

  public Optional<String> getQualifier() {
    return this.qualifier;
  }

  public String getCompositeStepId() {
    return this.id + (this.qualifier.isPresent() ? "[" + this.qualifier.get() + "]" : "");
  }

  @Override
  public String toString() {
    ToStringHelper toStringHelper = MoreObjects.toStringHelper(this);
    toStringHelper.add("id", this.id);
    toStringHelper.add("qualifier", this.qualifier.or("---"));
    return toStringHelper.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.id, this.qualifier.orNull());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SimpleWorkflowStep) {
      SimpleWorkflowStep otherStep = (SimpleWorkflowStep) other;
      return Objects.equal(this.id, otherStep.getStepId()) && Objects.equal(this.qualifier, otherStep.getQualifier());
    }
    return false;
  }

  @Override
  public boolean containsId(String id) {
    return Objects.equal(id, this.id);
  }
}
