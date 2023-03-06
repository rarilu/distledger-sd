package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;

public class UnknownAccountException extends OperationException {
  public UnknownAccountException(String account) {
    super(Status.NOT_FOUND.withDescription("Account " + account + " does not exist"));
  }
}
