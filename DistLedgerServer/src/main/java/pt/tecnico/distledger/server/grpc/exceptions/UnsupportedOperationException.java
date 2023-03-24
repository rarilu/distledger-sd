package pt.tecnico.distledger.server.grpc.exceptions;

/** Represents an exception thrown when any operation is attempted on a read-only server. */
public class UnsupportedOperationException extends RuntimeException {
  public UnsupportedOperationException() {
    super("Unsupported operation on read-only server");
  }
}
