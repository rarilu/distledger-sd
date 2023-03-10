package pt.tecnico.distledger.server.domain.exceptions;

/**
 * Represents an exception thrown when an account to be deleted is protected, e.g., it is a system
 * account, and thus cannot be deleted.
 */
public class ProtectedAccountException extends RuntimeException {
  public ProtectedAccountException(String userId) {
    super("Account for user " + userId + " is protected");
  }
}
