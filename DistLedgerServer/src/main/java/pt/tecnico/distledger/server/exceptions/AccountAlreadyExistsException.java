package pt.tecnico.distledger.server.exceptions;

import io.grpc.Status;

public class AccountAlreadyExistsException extends OperationException {
  public AccountAlreadyExistsException(String userId) {
    super(Status.ALREADY_EXISTS.withDescription("Account for user " + userId + " already exists"));
  }
}
