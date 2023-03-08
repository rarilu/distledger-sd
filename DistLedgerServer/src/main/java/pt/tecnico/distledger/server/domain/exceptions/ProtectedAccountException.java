package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;

public class ProtectedAccountException extends OperationException {
  public ProtectedAccountException(String userId) {
    super(Status.INVALID_ARGUMENT.withDescription("Account for user " + userId + " is protected"));
  }
}
