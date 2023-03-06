package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.OperationException;
import pt.tecnico.distledger.utils.Logger;

public class CreateOp extends Operation {
  public CreateOp(String account) {
    super(account);
  }

  @Override
  public void apply(ServerState state) throws OperationException {
    if (state.getAccounts().containsKey(this.getAccount())) {
      Logger.debug("Account " + this.getAccount() + " already exists");
      throw new AccountAlreadyExistsException(this.getAccount());
    } else {
      state.getAccounts().put(this.getAccount(), 0);
      Logger.debug("Created account " + this.getAccount());
    }
  }
}
