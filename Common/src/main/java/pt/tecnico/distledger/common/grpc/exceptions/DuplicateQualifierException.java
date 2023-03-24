package pt.tecnico.distledger.common.grpc.exceptions;

/**
 * Represents an exception thrown when there is more than one server with the same qualifier.
 *
 * <p>The current consistency model and base assumptions used in the DistLedger do not allow for
 * multiple servers to have the same qualifier. This exception is thrown when this is detected,
 * alerting for possibly inconsistent state.
 */
public class DuplicateQualifierException extends RuntimeException {
  /** Creates a new DuplicateQualifierException given the qualifier that multiple servers have. */
  public DuplicateQualifierException(String qualifier) {
    super(
        "More than one server found for the qualifier "
            + qualifier
            + " - this is not present in the consistency model");
  }
}
