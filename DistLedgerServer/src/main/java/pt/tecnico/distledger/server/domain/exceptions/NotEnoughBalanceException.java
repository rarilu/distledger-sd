package pt.tecnico.distledger.server.domain.exceptions;

public class NotEnoughBalanceException extends RuntimeException {
  public NotEnoughBalanceException(String account, int amount) {
    super("Account " + account + " does not have enough balance to transfer " + amount);
  }
}
