package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when there is an attempt to (re-)create a system account. */
public class SystemAccountException extends RuntimeException {
  public SystemAccountException(String userId) {
    super("Account for user " + userId + " is a system account and cannot be created");
  }
}
