package pt.tecnico.distledger.server.domain.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class NotEnoughBalanceException extends StatusRuntimeException {
  public NotEnoughBalanceException(String account, int amount) {
    super(
        Status.FAILED_PRECONDITION.withDescription(
            "Account " + account + " does not have enough balance to transfer " + amount));
  }
}
