package pt.tecnico.distledger.server.domain.operation;

import java.util.Objects;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.domain.exceptions.NonPositiveTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NopTransferException;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents a transfer operation. */
public class TransferOp extends Operation {
  private final String destUserId;
  private final int amount;

  /**
   * Creates a new transfer operation.
   *
   * @param fromUserId the user id of the account to transfer from
   * @param destUserId the user id of the account to transfer to
   * @param amount the amount to transfer
   * @param prevTimeStamp Client's timestamp when the operation was executed
   * @param timeStamp Unique timestamp of the operation
   * @throws NonPositiveTransferException if the amount is not positive
   * @throws NopTransferException if the source and destination accounts are the same
   */
  public TransferOp(
      String fromUserId,
      String destUserId,
      int amount,
      VectorClock prevTimeStamp,
      VectorClock timeStamp) {
    super(fromUserId, prevTimeStamp, timeStamp);
    this.destUserId = destUserId;
    this.amount = amount;

    if (amount <= 0) {
      throw new NonPositiveTransferException();
    }

    if (Objects.equals(fromUserId, destUserId)) {
      throw new NopTransferException();
    }
  }

  public String getDestUserId() {
    return this.destUserId;
  }

  public int getAmount() {
    return this.amount;
  }

  @Override
  public void accept(OperationVisitor visitor) {
    visitor.visit(this);
  }
}
