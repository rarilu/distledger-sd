package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class NopTransferException extends StatusRuntimeException {
  public NopTransferException() {
    super(
        Status.INVALID_ARGUMENT.withDescription(
            "Transfers from an account to itself are not allowed"));
  }
}
