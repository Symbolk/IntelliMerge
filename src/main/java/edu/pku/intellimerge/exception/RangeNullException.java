package edu.pku.intellimerge.exception;

import edu.pku.intellimerge.model.node.TerminalNode;

public class RangeNullException extends Exception {
  public RangeNullException(String message) {
    super(message);
  }

  public RangeNullException(String message, TerminalNode node) {
    super(getExceptionDetails(message, node));
  }

  public static String getExceptionDetails(String message, TerminalNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(message).append("\n");
    builder.append(node.toString()).append("\n");
    return builder.toString();
  }
}
