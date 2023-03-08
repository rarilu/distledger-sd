package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ProtectedAccountException extends StatusRuntimeException {
  public ProtectedAccountException(String userId) {
    super(Status.INVALID_ARGUMENT.withDescription("Account for user " + userId + " is protected"));
  }
}
