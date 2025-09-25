package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ParallelWorkflowStep.Builder;
import com.itemis.maven.plugins.cdi.logging.Logger;

import de.vandermeer.asciitable.AT_Context;
import de.vandermeer.asciitable.AT_Renderer;
import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.u8.U8_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

/**
 * A utility class all around workflow processing, except the actual execution of the workflows.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 2.0.0
 */
public class WorkflowUtil {

  public static final String CONTEXT_DATA_MAP_ASSIGNMENT = "=>";
  public static final String CONTEXT_DATA_SEPARATOR = ",";
  private static final String DEFAULT_WORKFLOW_DIR = "META-INF/workflows";

  /**
   * Parses a workflow from its descriptor representation.
   *
   * @param trimmedWorkflowLines the lines read from the workflow and trimmed.
   * @param goalName the name of the goal this workflow is designed for.
   * @return the parsed processing workflow.
   *
   * @since 4.0.2
   */
  // TODO rework parser! -> too many decision branches!
  public static ProcessingWorkflow parseWorkflow(List<String> trimmedWorkflowLines, String goalName) {
    ProcessingWorkflow workflow = new ProcessingWorkflow(goalName);

    Builder parallelStepBuilder = null;
    SimpleWorkflowStep currentStep = null;
    boolean isTryBlock = false;
    boolean isFinallyBlock = false;

    for (String line : trimmedWorkflowLines) {
      if (line.startsWith(WorkflowConstants.KW_COMMENT) || line.isEmpty()) {
        continue;
      }

      if (line.startsWith(WorkflowConstants.KW_TRY)) {
        isTryBlock = true;
        isFinallyBlock = false;
      } else if (line.startsWith(WorkflowConstants.KW_PARALLEL)) {
        if (isFinallyBlock) {
          throw new RuntimeException(
              "Parallel block is not supported within finally-block. Processed line was: '" + line + "'");
        }
        parallelStepBuilder = ParallelWorkflowStep.builder();
      } else if (Objects.equal(WorkflowConstants.KW_BLOCK_CLOSE, line)) {
        if (currentStep != null) {
          currentStep = null;
        } else if (parallelStepBuilder != null) {
          workflow.addProcessingStep(parallelStepBuilder.build());
          parallelStepBuilder = null;
        } else if (isFinallyBlock) {
          isFinallyBlock = false;
        }
      } else if (line.startsWith(WorkflowConstants.KW_BLOCK_CLOSE)) {
        if (isTryBlock) {
          isTryBlock = false;
          String substring = line.substring(1).trim();
          if (substring.startsWith(WorkflowConstants.KW_FINALLY)
              && substring.endsWith(WorkflowConstants.KW_BLOCK_OPEN)) {
            isFinallyBlock = true;
          }
        }
      } else {
        if (currentStep == null) {
          String id = parseId(line);
          Optional<String> qualifier = parseQualifier(line);
          SimpleWorkflowStep step = new SimpleWorkflowStep(id, qualifier);
          if (line.endsWith(WorkflowConstants.KW_BLOCK_OPEN)) {
            currentStep = step;
          }

          if (isFinallyBlock) {
            workflow.addFinallyStep(step);
          } else {
            if (parallelStepBuilder == null) {
              workflow.addProcessingStep(step);
            } else {
              parallelStepBuilder.addSteps(step);
            }
          }
        } else {
          setDefaultExecutionData(currentStep, line);
          setDefaultRollbackData(currentStep, line);
        }
      }
    }

    return workflow;
  }

  /**
   * Parses a workflow from its descriptor representation.
   *
   * @param is the input stream to read the workflow descriptor from. This stream will be closed after reading the
   *          workflow descriptor.
   * @param goalName the name of the goal this workflow is designed for.
   * @return the parsed processing workflow.
   * @deprecated Use {@link #parseWorkflow(List, String)} instead.
   */
  @Deprecated
  public static ProcessingWorkflow parseWorkflow(InputStream is, String goalName) {
    List<String> trimmedWorkflowLines;
    try {
      trimmedWorkflowLines = getTrimmedWorkflowLines(is);
    } catch (MojoExecutionException e) {
      throw new RuntimeException("Unable to read the workflow descriptor from the provided input stream.", e);
    }
    return parseWorkflow(trimmedWorkflowLines, goalName);
  }

  private static String parseId(String line) {
    int qualifierOpen = line.indexOf(WorkflowConstants.KW_QUALIFIER_OPEN);
    int blockOpen = line.indexOf(WorkflowConstants.KW_BLOCK_OPEN);
    int toIndex;
    if (qualifierOpen > -1) {
      toIndex = qualifierOpen;
    } else if (blockOpen > -1) {
      toIndex = blockOpen;
    } else {
      toIndex = line.length();
    }
    return line.substring(0, toIndex).trim();
  }

  private static Optional<String> parseQualifier(String line) {
    String qualifier = null;
    int qualifierOpen = line.indexOf(WorkflowConstants.KW_QUALIFIER_OPEN);
    if (qualifierOpen > -1) {
      int qualifierClose = line.indexOf(WorkflowConstants.KW_QUALIFIER_CLOSE, qualifierOpen);
      qualifier = line.substring(qualifierOpen + 1, qualifierClose);
    }
    return Optional.fromNullable(qualifier);
  }

  private static void setDefaultExecutionData(SimpleWorkflowStep step, String line) {
    if (line.startsWith(WorkflowConstants.KW_DATA)) {
      int startIndex = line.indexOf(WorkflowConstants.KW_DATA_ASSIGNMENT) + 1;
      step.setDefaultExecutionData(line.substring(startIndex).trim());
    }
  }

  private static void setDefaultRollbackData(SimpleWorkflowStep step, String line) {
    if (line.startsWith(WorkflowConstants.KW_ROLLBACK_DATA)) {
      int startIndex = line.indexOf(WorkflowConstants.KW_DATA_ASSIGNMENT) + 1;
      step.setDefaultRollbackData(line.substring(startIndex).trim());
    }
  }

  public static void addExecutionContexts(ProcessingWorkflow workflow) {
    Iterable<WorkflowStep> steps = Iterables
        .unmodifiableIterable(Iterables.concat(workflow.getProcessingSteps(), workflow.getFinallySteps()));
    for (WorkflowStep step : steps) {
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
    if (dataPropertyValue == null) {
      dataPropertyValue = step.getDefaultExecutionData().orNull();
    }
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
    if (dataRollbackPropertyValue == null) {
      dataRollbackPropertyValue = step.getDefaultRollbackData().orNull();
    }
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

  private static InputStream getWorkflowInputStream(String goalName, PluginDescriptor pluginDescriptor,
      Optional<File> customWorkflowDescriptor, Logger log) throws MojoExecutionException {
    log.info("Constructing workflow for processing");
    String goalPrefix = pluginDescriptor.getGoalPrefix();

    if (customWorkflowDescriptor.isPresent()) {
      File customDescriptor = customWorkflowDescriptor.get();
      log.debug("Requested overriding of workflow with file: " + customDescriptor.getAbsolutePath());

      if (customDescriptor.exists() && customDescriptor.isFile()) {
        try {
          log.info("Default workflow of goal '" + goalPrefix + ':' + goalName + "' will be overridden by file '"
              + customDescriptor.getAbsolutePath() + "'.");
          return new FileInputStream(customDescriptor);
        } catch (Exception e) {
          throw new MojoExecutionException("Unable to load custom workflow for goal " + goalName, e);
        }
      } else {
        throw new MojoExecutionException("Unable to load custom workflow for goal " + goalPrefix + ':' + goalName
            + ". The workflow file '" + customDescriptor.getAbsolutePath() + "' does not exist!");
      }
    }

    log.info("Goal '" + goalPrefix + ':' + goalName + "' will use default workflow packaged with the plugin.");
    return getResourceStream(DEFAULT_WORKFLOW_DIR + "/" + goalName);
  }

  /**
   * @param resourcePath the path to the resource file
   * @return {@link InputStream} to read the workflow from a resource file
   * @throws MojoExecutionException if resource file does not exist
   *
   * @since 4.0.2
   */
  public static InputStream getResourceStream(String resourcePath) throws MojoExecutionException {
    InputStream ret = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    if (ret == null) {
      throw new MojoExecutionException("Resource for path '" + resourcePath + "' does not exist!");
    }
    return ret;
  }

  /**
   * @param goalName the current goal being used
   * @param pluginDescriptor the descriptor of the plugin calling
   * @param customWorkflowDescriptor the workflow descriptor referring to a workflow file
   * @param log the logger passed from the caller
   * @return the {@link InputStream} for reading the workflow resource or file
   * @throws MojoExecutionException in case of workflow file not existing
   * @deprecated Use {@link #getTrimmedWorkflowLines(String, PluginDescriptor, Optional, Logger)} instead.
   */
  @Deprecated
  public static InputStream getWorkflowDescriptor(String goalName, PluginDescriptor pluginDescriptor,
      Optional<File> customWorkflowDescriptor, Logger log) throws MojoExecutionException {
    return getWorkflowInputStream(goalName, pluginDescriptor, customWorkflowDescriptor, log);
  }

  public static List<String> getTrimmedWorkflowLines(InputStream is) throws MojoExecutionException {
    List<String> ret = Lists.newArrayList();
    try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String line;
      while ((line = br.readLine()) != null) {
        ret.add(line.trim());
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to read the workflow from the input stream provided.", e);
    }
    return ret;
  }

  /**
   * @param goalName the current goal being used
   * @param pluginDescriptor the descriptor of the plugin calling
   * @param customWorkflowDescriptor the workflow descriptor referring to a workflow file
   * @param log the logger passed from the caller
   * @return list of workflow lines trimmed for white spaces
   * @throws MojoExecutionException in case of workflow file not existing
   *
   * @since 4.0.2
   */
  public static List<String> getTrimmedWorkflowLines(String goalName, PluginDescriptor pluginDescriptor,
      Optional<File> customWorkflowDescriptor, Logger log) throws MojoExecutionException {
    return getTrimmedWorkflowLines(getWorkflowInputStream(goalName, pluginDescriptor, customWorkflowDescriptor, log));
  }

  public static void printWorkflow(String goalName, PluginDescriptor pluginDescriptor,
      Optional<File> customWorkflowDescriptor, Logger log) throws MojoExecutionException {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(pluginDescriptor.getGoalPrefix())) {
      sb.append(pluginDescriptor.getGoalPrefix());
    } else {
      sb.append(pluginDescriptor.getGroupId()).append(':').append(pluginDescriptor.getArtifactId()).append(':')
          .append(pluginDescriptor.getVersion());
    }
    sb.append(':').append(goalName);

    log.info("Default workflow for '" + sb + "':");

    List<String> trimmedWorkflowLines = getTrimmedWorkflowLines(goalName, pluginDescriptor, customWorkflowDescriptor,
        log);
    int x = 77 - goalName.length();
    int a = x / 2;
    int b = x % 2 == 1 ? a + 1 : a;
    StringBuilder separator = new StringBuilder();
    separator.append(Strings.repeat("=", a)).append(' ').append(goalName).append(' ').append(Strings.repeat("=", b));

    System.out.println(separator);
    trimmedWorkflowLines.forEach(line -> {
      System.out.println(line);
    });
    System.out.println(separator);
  }

  public static void printAvailableSteps(Map<String, ProcessingStep> steps, Logger log) {
    log.info(
        "The following processing steps are available on classpath and can be configured as part of a custom workflow.");
    System.out.println(renderAvailableSteps(steps));
  }

  public static String renderAvailableSteps(Map<String, ProcessingStep> steps) {
    AsciiTable table = new AsciiTable(new AT_Context().setGrid(U8_Grids.borderStrongDoubleLight()));
    table.addRule();
    AT_Row header = table.addRow("ID", "DESCRIPTION", "REQUIRES ONLINE");
    header.setTextAlignment(TextAlignment.CENTER);
    table.addStrongRule();

    List<String> sortedIds = Lists.newArrayList(steps.keySet());
    Collections.sort(sortedIds);
    for (String id : sortedIds) {
      ProcessingStep annotation = steps.get(id);
      AT_Row data = table.addRow(annotation.id(), annotation.description(), annotation.requiresOnline());
      data.setTextAlignment(TextAlignment.CENTER).setPaddingLeftRight(1);
      data.getCells().get(0).getContext().setTextAlignment(TextAlignment.LEFT);
      data.getCells().get(1).getContext().setTextAlignment(TextAlignment.LEFT);
      table.addRule();
    }

    // renderer.setTheme(AT_Themes.UTF_STRONG_DOUBLE.get());
    return table.setRenderer(AT_Renderer.create().setCWC(new CWC_LongestLine().add(10, 20).add(20, 50).add(10, 10)))
        .render();
  }

}
