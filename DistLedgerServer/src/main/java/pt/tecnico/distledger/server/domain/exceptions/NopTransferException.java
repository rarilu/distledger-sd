package pt.tecnico.distledger.server.domain.exceptions;

public class NopTransferException extends RuntimeException {
  public NopTransferException() {
    super("Transfers from an account to itself are not allowed");
  }
}
