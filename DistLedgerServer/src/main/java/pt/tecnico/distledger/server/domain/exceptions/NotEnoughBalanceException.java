package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;

public class NotEnoughBalanceException extends OperationException {
  public NotEnoughBalanceException(String account, int amount) {
    super(
        Status.FAILED_PRECONDITION.withDescription(
            "Account " + account + " does not have enough balance to transfer " + amount));
  }
}
