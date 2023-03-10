package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when a transfer from an account to itself is attempted. */
public class NopTransferException extends RuntimeException {
  public NopTransferException() {
    super("Transfers from an account to itself are not allowed");
  }
}
