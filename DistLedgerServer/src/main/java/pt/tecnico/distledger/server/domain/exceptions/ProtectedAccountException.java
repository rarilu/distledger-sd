package pt.tecnico.distledger.server.domain.exceptions;

public class ProtectedAccountException extends RuntimeException {
  public ProtectedAccountException(String userId) {
    super("Account for user " + userId + " is protected");
  }
}
