package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitors.OperationExecutor;

public class ServerState {
  private List<Operation> ledger = new ArrayList<>();
  private Map<String, Account> accounts = new HashMap<>();
  private OperationExecutor executor;

  public ServerState() {
    this.accounts.put("broker", new Account(1000));
    this.executor = new OperationExecutor(this);
  }

  public synchronized void registerOperation(Operation op) {
    op.accept(this.executor);
    this.ledger.add(op);
  }

  public Map<String, Account> getAccounts() {
    return this.accounts;
  }
}
