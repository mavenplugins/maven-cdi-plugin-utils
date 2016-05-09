package com.itemis.maven.plugins.cdi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.base.Objects;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.cdi.workflow.ProcessingWorkflow;
import com.itemis.maven.plugins.cdi.workflow.WorkflowStep;
import com.itemis.maven.plugins.cdi.workflow.WorkflowStep.Builder;

public class WorkflowUtil {
  public static final String KW_COMMENT = "#";
  public static final String KW_PARALLEL = "parallel";
  public static final String KW_BLOCK_OPEN = "{";
  public static final String KW_BLOCK_CLOSE = "}";

  public static ProcessingWorkflow parseWorkflow(InputStream is, String goalName) {
    ProcessingWorkflow workflow = new ProcessingWorkflow(goalName);

    // TODO rework parser! specify format and handle failures
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(is));
      String line;
      Builder parallelStepBuilder = null;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith(KW_COMMENT)) {
          continue;
        }

        if (line.startsWith(KW_PARALLEL)) {
          parallelStepBuilder = WorkflowStep.parallel();
        } else if (Objects.equal(KW_BLOCK_CLOSE, line) && parallelStepBuilder != null) {
          workflow.addProcessingStep(parallelStepBuilder.build());
        } else {
          if (parallelStepBuilder == null) {
            workflow.addProcessingStep(WorkflowStep.sequencial().setSequencialStep(line).build());
          } else {
            parallelStepBuilder.addParallelSteps(line);
          }
        }
      }
    } catch (IOException e) {
      // FIXME handle!
    } finally {
      Closeables.closeQuietly(br);
    }

    return workflow;
  }
}
