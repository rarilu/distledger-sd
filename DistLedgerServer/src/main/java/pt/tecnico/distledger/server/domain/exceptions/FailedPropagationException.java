package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when an attempt to propagate state fails */
public class FailedPropagationException extends RuntimeException {
  public FailedPropagationException() {
    super("Failed to propagate state to server");
  }
}
