package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when an account to be created already exists. */
public class AccountAlreadyExistsException extends RuntimeException {
  public AccountAlreadyExistsException(String userId) {
    super("Account for user " + userId + " already exists");
  }
}
