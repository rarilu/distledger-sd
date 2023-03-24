package pt.tecnico.distledger.common.grpc.exceptions;

/** Represents an exception thrown when no server is found for a given qualifier. */
public class ServerNotFoundException extends RuntimeException {
  public ServerNotFoundException(String qualifier) {
    super("No server found for the qualifier " + qualifier);
  }
}
