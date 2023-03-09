package pt.tecnico.distledger.server.domain.exceptions;

public class AccountAlreadyExistsException extends RuntimeException {
  public AccountAlreadyExistsException(String userId) {
    super("Account for user " + userId + " already exists");
  }
}
