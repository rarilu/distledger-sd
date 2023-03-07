package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.exceptions.OperationException;

public class ServerState {
  private List<Operation> ledger = new ArrayList<>();
  private Map<String, Integer> accounts = new HashMap<>();

  public ServerState() {
    this.accounts.put("broker", 1000);
  }

  public synchronized void registerOperation(Operation op) throws OperationException {
    op.apply(this);
    this.ledger.add(op);
  }

  public Map<String, Integer> getAccounts() {
    return this.accounts;
  }
}
