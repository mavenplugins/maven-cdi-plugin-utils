package com.itemis.maven.plugins.cdi.internal.util.workflow;

public interface WorkflowStep {
  boolean isParallel();

  boolean containsId(String id);
}
