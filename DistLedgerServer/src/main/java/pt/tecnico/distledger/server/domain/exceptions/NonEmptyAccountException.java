package pt.tecnico.distledger.server.domain.exceptions;

public class NonEmptyAccountException extends RuntimeException {
  public NonEmptyAccountException(String userId, int amount) {
    super("Account for user " + userId + " has " + amount + " left, needs to be empty");
  }
}
