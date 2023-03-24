package pt.tecnico.distledger.common.grpc.exceptions;

/** Represents an exception thrown when there is more than one server with the same qualifier. */
public class DuplicateQualifierException extends RuntimeException {
  public DuplicateQualifierException(String qualifier) {
    super(
        "More than one server found for the qualifier "
            + qualifier
            + " - this is not present in the consistency model");
  }
}
