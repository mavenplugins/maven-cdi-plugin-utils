package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.base.Objects;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.cdi.internal.util.workflow.WorkflowStep.Builder;

/**
 * A utility class all around workflow processing, except the actual execution of the workflows.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public class WorkflowUtil {
  public static final String KW_COMMENT = "#";
  public static final String KW_PARALLEL = "parallel";
  public static final String KW_BLOCK_OPEN = "{";
  public static final String KW_BLOCK_CLOSE = "}";

  /**
   * Parses a workflow from its descriptor representation.
   *
   * @param is the input stream to read the workflow descriptor from. This stream will be closed after reading the
   *          workflow descriptor.
   * @param goalName the name of the goal this workflow is designed for.
   * @return the parsed processing workflow.
   */
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
            workflow.addProcessingStep(WorkflowStep.sequential().setSequentialStep(line).build());
          } else {
            parallelStepBuilder.addParallelSteps(line);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the workflow descriptor from the provided input stream.", e);
    } finally {
      Closeables.closeQuietly(br);
    }

    return workflow;
  }
}
