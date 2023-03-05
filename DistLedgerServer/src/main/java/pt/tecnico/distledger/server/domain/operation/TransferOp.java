package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.OperationException;

public class TransferOp extends Operation {
  private String destAccount;
  private int amount;

  public TransferOp(String fromAccount, String destAccount, int amount) {
    super(fromAccount);
    this.destAccount = destAccount;
    this.amount = amount;
  }

  public String getDestAccount() {
    return destAccount;
  }

  public void setDestAccount(String destAccount) {
    this.destAccount = destAccount;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  @Override
  public void apply(ServerState state) throws OperationException {
    // TODO
  }
}
