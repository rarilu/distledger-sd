package pt.tecnico.distledger.namingserver.domain.exceptions;

/** Represents an exception thrown when a server entry to be created already exists. */
public class DuplicateServerEntryException extends RuntimeException {

  /**
   * Creates a new DuplicateServerEntryException given details of the server entry whose creation
   * was attempted.
   */
  public DuplicateServerEntryException(String service, String qualifier, String target) {
    super(
        "An entry in service "
            + service
            + " for target "
            + target
            + " or qualifier "
            + qualifier
            + " already exists");
  }
}
