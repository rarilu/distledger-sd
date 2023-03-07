package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.OperationException;

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
  public void apply(ServerState state) throws OperationException {
    // TODO
  }
}
