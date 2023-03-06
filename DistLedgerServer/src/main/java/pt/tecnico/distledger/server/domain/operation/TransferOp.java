package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.exceptions.OperationException;
import pt.tecnico.distledger.server.exceptions.UnknownAccountException;
import pt.tecnico.distledger.utils.Logger;

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
    // Get the accounts.
    Account fromAccount = state.getAccounts().get(this.getUserId());
    Account destAccount = state.getAccounts().get(this.getDestUserId());

    // Check if the accounts exist.
    if (fromAccount == null) {
      Logger.debug("Account " + this.getUserId() + " does not exist");
      throw new UnknownAccountException(this.getUserId());
    }

    if (destAccount == null) {
      Logger.debug("Account " + this.getDestUserId() + " does not exist");
      throw new UnknownAccountException(this.getDestUserId());
    }

    // Check if the account has enough money.
    if (fromAccount.getBalance() < this.getAmount()) {
      Logger.debug("Account " + this.getUserId() + " does not have enough balance");
      throw new NotEnoughBalanceException(this.getUserId(), this.getAmount());
    }

    // Transfer the money.
    fromAccount.setBalance(fromAccount.getBalance() - this.getAmount());
    destAccount.setBalance(destAccount.getBalance() + this.getAmount());

    Logger.debug(
        "Transferred "
            + this.getAmount()
            + " from "
            + this.getUserId()
            + " to "
            + this.getDestUserId());
  }
}
