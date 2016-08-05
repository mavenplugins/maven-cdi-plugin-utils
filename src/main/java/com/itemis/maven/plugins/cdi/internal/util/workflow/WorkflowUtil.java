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
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.itemis.maven.plugins.cdi.ExecutionContext;
import com.itemis.maven.plugins.cdi.annotations.ProcessingStep;
import com.itemis.maven.plugins.cdi.internal.util.workflow.ParallelWorkflowStep.Builder;
import com.itemis.maven.plugins.cdi.logging.Logger;

import de.vandermeer.asciitable.v2.RenderedTable;
import de.vandermeer.asciitable.v2.V2_AsciiTable;
import de.vandermeer.asciitable.v2.render.V2_AsciiTableRenderer;
import de.vandermeer.asciitable.v2.render.WidthLongestLine;
import de.vandermeer.asciitable.v2.row.ContentRow;
import de.vandermeer.asciitable.v2.themes.V2_E_TableThemes;

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

  private static final String DEFAULT_WORKFLOW_DIR = "META-INF/workflows";

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

  public static InputStream getWorkflowDescriptor(String goalName, PluginDescriptor pluginDescriptor,
      Optional<File> customWorkflowDescriptor, Logger log) throws MojoExecutionException {
    log.info("Constructing workflow for processing");
    String goalPrefix = pluginDescriptor.getGoalPrefix();

    if (customWorkflowDescriptor.isPresent()) {
      File customDescriptor = customWorkflowDescriptor.get();
      log.debug("Requested overriding of workflow with file: " + customDescriptor.getAbsolutePath());

      if (customDescriptor.exists() && customDescriptor.isFile()) {
        try {
          log.info("Workflow of goal '" + goalPrefix + ':' + goalName + "' will be overriden by file '"
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
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_WORKFLOW_DIR + "/" + goalName);
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

    InputStream workflowDescriptor = getWorkflowDescriptor(goalName, pluginDescriptor, customWorkflowDescriptor, log);
    try {
      int x = 77 - goalName.length();
      int a = x / 2;
      int b = x % 2 == 1 ? a + 1 : a;
      StringBuilder separator = new StringBuilder();
      separator.append(Strings.repeat("=", a)).append(' ').append(goalName).append(' ').append(Strings.repeat("=", b));

      System.out.println(separator);
      ByteStreams.copy(workflowDescriptor, System.out);
      System.out.println(separator);
    } catch (IOException e) {
      throw new MojoExecutionException("A problem occurred during the serialization of the defualt workflow.", e);
    } finally {
      Closeables.closeQuietly(workflowDescriptor);
    }
  }

  public static boolean printAvailableSteps(Map<String, ProcessingStep> steps, Logger log)
      throws MojoExecutionException {
    V2_AsciiTable table = new V2_AsciiTable();
    table.addRule();
    ContentRow header = table.addRow("ID", "DESCRIPTION", "REQUIRES ONLINE");
    header.setAlignment(new char[] { 'c', 'c', 'c' });
    table.addStrongRule();

    List<String> sortedIds = Lists.newArrayList(steps.keySet());
    Collections.sort(sortedIds);
    for (String id : sortedIds) {
      ProcessingStep annotation = steps.get(id);
      ContentRow data = table.addRow(annotation.id(), annotation.description(), annotation.requiresOnline());
      data.setAlignment(new char[] { 'l', 'l', 'c' });
      table.addRule();
    }

    V2_AsciiTableRenderer renderer = new V2_AsciiTableRenderer();
    renderer.setTheme(V2_E_TableThemes.UTF_STRONG_DOUBLE.get());
    renderer.setWidth(new WidthLongestLine().add(10, 20).add(20, 50).add(10, 10));
    RenderedTable renderedTable = renderer.render(table);

    log.info(
        "The following processing steps are available on classpath and can be configured as part of a custom workflow.");
    System.out.println(renderedTable);

    return true;
  }
}
