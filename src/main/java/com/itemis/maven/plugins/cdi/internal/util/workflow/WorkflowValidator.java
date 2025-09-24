package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.util.List;

/**
 * A utility class for workflow validation.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 3.2.0
 */
public class WorkflowValidator {

  public static void validateSyntactically(List<String> trimmedWorkflowLines) {
    boolean isTryBlockOpen = false;
    boolean isFinallyBlockOpen = false;
    int nrOfOpenTryBlocks = 0;

    int lineNumber = 0;
    for (String line : trimmedWorkflowLines) {
      if (line.startsWith(WorkflowConstants.KW_COMMENT) || line.isEmpty()) {
        continue;
      }
      // line number is only increased for non-comment lines
      lineNumber++;

      isTryBlockOpen = validateTryBlockOpening(line, lineNumber);
      if (isTryBlockOpen) {
        nrOfOpenTryBlocks++;
      }
      isFinallyBlockOpen = validateFinallyBlockOpening(line);
      if (isFinallyBlockOpen) {
        if (nrOfOpenTryBlocks > 0) {
          nrOfOpenTryBlocks--;
        } else {
          throw new RuntimeException(
              "There is a finally-block opening without a try-block releated. Processed line was: '" + line + "'");
        }
      }
    }
    if (nrOfOpenTryBlocks > 0) {
      throw new RuntimeException("There are try-blocks missing a closing finally-block.");
    }
  }

  private static boolean validateTryBlockOpening(String line, int lineNumber) {
    if (line.contains(WorkflowConstants.KW_TRY)) {
      if (!line.startsWith(WorkflowConstants.KW_TRY)) {
        throw new RuntimeException(
            "Opening the try-block requires the keyword 'try' to be the first token of the line. Processed line was: '"
                + line + "'");
      }
      if (lineNumber != 1) {
        throw new RuntimeException(
            "The try-block opening must be the first statement. Only comments are allowed to occur before opening the try-block. Processed line was: '"
                + line + "'");
      }
      String remainingContent = line.substring(WorkflowConstants.KW_TRY.length()).trim();
      if (!remainingContent.equals(WorkflowConstants.KW_BLOCK_OPEN)) {
        throw new RuntimeException("The try block opening must end with the block opening character '"
            + WorkflowConstants.KW_BLOCK_OPEN + "'. Processed line was: '" + line + "'");
      }
      return true;
    }
    return false;
  }

  private static boolean validateFinallyBlockOpening(String line) {
    if (line.startsWith(WorkflowConstants.KW_BLOCK_CLOSE)) {
      String remainingContent = line.substring(1).trim();
      if (remainingContent.startsWith(WorkflowConstants.KW_FINALLY)) {
        remainingContent = remainingContent.substring(WorkflowConstants.KW_FINALLY.length()).trim();
        if (!remainingContent.equals(WorkflowConstants.KW_BLOCK_OPEN)) {
          throw new RuntimeException("The finally block opening must end with the block opening character '"
              + WorkflowConstants.KW_BLOCK_OPEN + "'. Processed line was: '" + line + "'");
        }
        return true;
      }
    }
    return false;
  }
}
