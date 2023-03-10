package pt.tecnico.distledger.server.domain.exceptions;

/** Represents an exception thrown when the server is unavailable to handle client requests. */
public class ServerUnavailableException extends RuntimeException {
  public ServerUnavailableException() {
    super("Server is unavailable");
  }
}
