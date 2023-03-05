package pt.tecnico.distledger.server.exceptions;

public class AccountAlreadyExistsException extends OperationException {
  public AccountAlreadyExistsException(String userId) {
    super("Account for user " + userId + " already exists");
  }
}
