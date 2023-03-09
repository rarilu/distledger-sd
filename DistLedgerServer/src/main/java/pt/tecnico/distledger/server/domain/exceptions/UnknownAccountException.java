package pt.tecnico.distledger.server.domain.exceptions;

public class UnknownAccountException extends RuntimeException {
  public UnknownAccountException(String account) {
    super("Account " + account + " does not exist");
  }
}
