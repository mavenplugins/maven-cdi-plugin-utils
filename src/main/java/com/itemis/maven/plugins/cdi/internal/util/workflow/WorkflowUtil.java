package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ParallelWorkflowStep.Builder;

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
  public static final String KW_QUALIFIER_OPEN = "[";
  public static final String KW_QUALIFIER_CLOSE = "]";
  public static final String CONTEXT_DATA_MAP_ASSIGNMENT = "=>";
  public static final String CONTEXT_DATA_SEPARATOR = ",";

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
        if (line.startsWith(KW_COMMENT) || Strings.isNullOrEmpty(line)) {
          continue;
        }

        if (line.startsWith(KW_PARALLEL)) {
          parallelStepBuilder = ParallelWorkflowStep.builder();
        } else if (Objects.equal(KW_BLOCK_CLOSE, line) && parallelStepBuilder != null) {
          workflow.addProcessingStep(parallelStepBuilder.build());
        } else {
          int qualifierOpen = line.indexOf(KW_QUALIFIER_OPEN);
          SimpleWorkflowStep step;
          if (qualifierOpen > -1) {
            String id = line.substring(0, qualifierOpen);
            int qualifierClose = line.indexOf(KW_QUALIFIER_CLOSE, qualifierOpen);
            Optional<String> qualifier = Optional.of(line.substring(qualifierOpen + 1, qualifierClose));
            step = new SimpleWorkflowStep(id, qualifier);
          } else {
            step = new SimpleWorkflowStep(line, Optional.<String> absent());
          }

          if (parallelStepBuilder == null) {
            workflow.addProcessingStep(step);
          } else {
            parallelStepBuilder.addSteps(step);
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

  public static void addExecutionContexts(ProcessingWorkflow workflow) {
    for (WorkflowStep step : workflow.getProcessingSteps()) {
      if (step.isParallel()) {
        ParallelWorkflowStep parallelStep = (ParallelWorkflowStep) step;
        for (SimpleWorkflowStep simpleStep : parallelStep.getSteps()) {
          workflow.addExecutionContext(simpleStep.getCompositeStepId(), createExecutionContext(simpleStep));
        }
      } else {
        SimpleWorkflowStep simpleStep = (SimpleWorkflowStep) step;
        workflow.addExecutionContext(simpleStep.getCompositeStepId(), createExecutionContext(simpleStep));
      }
    }
  }

  private static ExecutionContext createExecutionContext(SimpleWorkflowStep step) {
    com.itemis.maven.plugins.cdi.ExecutionContext.Builder builder = ExecutionContext.builder(step.getStepId());
    if (step.getQualifier().isPresent()) {
      builder.setQualifier(step.getQualifier().get());
    }

    String dataPropertyValue = System.getProperty(step.getCompositeStepId());
    if (dataPropertyValue != null) {
      Iterable<String> split = Splitter.on(CONTEXT_DATA_SEPARATOR).split(dataPropertyValue);
      for (String token : split) {
        String date = Strings.emptyToNull(token.trim());
        if (date != null) {
          List<String> dataSplit = Splitter.on(CONTEXT_DATA_MAP_ASSIGNMENT).splitToList(date);
          if (dataSplit.size() == 1) {
            builder.addData(dataSplit.get(0));
          } else {
            builder.addData(dataSplit.get(0), dataSplit.get(1));
          }
        }
      }
    }

    String dataRollbackPropertyValue = System.getProperty(step.getCompositeStepId() + "-rollback");
    if (dataRollbackPropertyValue != null) {
      Iterable<String> split = Splitter.on(CONTEXT_DATA_SEPARATOR).split(dataRollbackPropertyValue);
      for (String token : split) {
        String date = Strings.emptyToNull(token.trim());
        if (date != null) {
          List<String> dataSplit = Splitter.on(CONTEXT_DATA_MAP_ASSIGNMENT).splitToList(date);
          if (dataSplit.size() == 1) {
            builder.addRollbackData(dataSplit.get(0));
          } else {
            builder.addRollbackData(dataSplit.get(0), dataSplit.get(1));
          }
        }
      }
    }

    return builder.build();
  }
}
