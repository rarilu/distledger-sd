package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.visitors.OperationVisitor;

public class TransferOp extends Operation {
  private String destUserId;
  private int amount;

  public TransferOp(String fromUserId, String destUserId, int amount) {
    super(fromUserId);
    this.destUserId = destUserId;
    this.amount = amount;
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
