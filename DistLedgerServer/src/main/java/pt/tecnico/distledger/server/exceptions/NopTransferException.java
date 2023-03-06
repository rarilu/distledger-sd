package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;

public class NopTransferException extends OperationException {
  public NopTransferException() {
    super(
        Status.INVALID_ARGUMENT.withDescription(
            "Transfers from an account to itself are not allowed"));
  }
}
