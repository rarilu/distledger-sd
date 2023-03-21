package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when a write operation is attempted on a read-only server. */
public class InvalidWriteOperationException extends RuntimeException {
  public InvalidWriteOperationException() {
    super("Invalid write operation on read-only server");
  }
}
