package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.OperationException;
import pt.tecnico.distledger.utils.Logger;

public class CreateOp extends Operation {
  public CreateOp(String userId) {
    super(userId);
  }

  @Override
  public void apply(ServerState state) throws OperationException {
    if (state.getAccounts().containsKey(this.getUserId())) {
      Logger.debug("Account " + this.getUserId() + " already exists");
      throw new AccountAlreadyExistsException(this.getUserId());
    } else {
      state.getAccounts().put(this.getUserId(), new Account());
      Logger.debug("Created account for " + this.getUserId());
    }
  }
}
