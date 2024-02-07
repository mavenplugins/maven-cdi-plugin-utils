package com.itemis.maven.plugins.cdi.internal.util.workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.io.Closeables;

/**
 * A utility class for workflow validation.
 *
 * @author <a href="mailto:stanley.hillner@itemis.de">Stanley Hillner</a>
 * @since 3.2.0
 */
public class WorkflowValidator {

  public static void validateSyntactically(InputStream is) {
    BufferedReader br = null;
    try {
      boolean isTryBlockOpen = false;
      boolean isFinallyBlockOpen = true;

      br = new BufferedReader(new InputStreamReader(is));
      String line;
      int lineNumber = 0;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith(WorkflowConstants.KW_COMMENT) || line.isEmpty()) {
          continue;
        }
        // line number is only increased for non-comment lines
        lineNumber++;

        isTryBlockOpen = validateTryBlockOpening(line, lineNumber);
        isFinallyBlockOpen = validateFinallyBlockOpening(line, isTryBlockOpen);
        if (isFinallyBlockOpen) {
          isTryBlockOpen = false;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the workflow descriptor from the provided input stream.", e);
    } finally {
      Closeables.closeQuietly(br);
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

  private static boolean validateFinallyBlockOpening(String line, boolean isTryBlockOpen) {
    if (line.startsWith(WorkflowConstants.KW_BLOCK_CLOSE)) {
      String remainingContent = line.substring(1).trim();
      if (line.startsWith(WorkflowConstants.KW_FINALLY)) {
        remainingContent = line.substring(WorkflowConstants.KW_FINALLY.length()).trim();
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
