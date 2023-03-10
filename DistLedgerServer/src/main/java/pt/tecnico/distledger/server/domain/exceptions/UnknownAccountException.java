package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when a non-existent account is attempted to be accessed. */
public class UnknownAccountException extends RuntimeException {
  public UnknownAccountException(String account) {
    super("Account " + account + " does not exist");
  }
}
