package pt.tecnico.distledger.namingserver.domain.exceptions;

/** Represents an exception thrown when a server entry is not found. */
public class ServerEntryNotFoundException extends RuntimeException {

  /**
   * Creates a new ServerEntryNotFoundException given details of the server entry whose absence
   * caused the exception.
   */
  public ServerEntryNotFoundException(String service, String target) {
    super(
        "An entry for server with target " + target + " and service " + service + " was not found");
  }
}
