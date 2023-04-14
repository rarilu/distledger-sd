package pt.tecnico.distledger.server.grpc.exceptions;

/** Represents an exception thrown when an attempt to propagate state fails. */
public class FailedPropagationException extends RuntimeException {
  public FailedPropagationException(Exception cause) {
    super(cause.getMessage(), cause);
  }
}
