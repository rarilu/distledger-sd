package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.OperationException;

public abstract class Operation {
  private String account;

  public Operation(String fromAccount) {
    this.account = fromAccount;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public abstract void apply(ServerState state) throws OperationException;
}
