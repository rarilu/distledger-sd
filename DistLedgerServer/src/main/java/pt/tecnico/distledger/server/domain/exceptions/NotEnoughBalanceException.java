package pt.tecnico.distledger.server.domain.exceptions;

/**
 * Represents an exception thrown when an account does not have enough balance to transfer a given
 * amount.
 */
public class NotEnoughBalanceException extends RuntimeException {
  public NotEnoughBalanceException(String account, int amount) {
    super("Account " + account + " does not have enough balance to transfer " + amount);
  }
}
