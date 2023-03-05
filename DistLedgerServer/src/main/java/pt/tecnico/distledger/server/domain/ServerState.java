package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pt.tecnico.distledger.server.domain.operation.Operation;

public class ServerState {
  private List<Operation> ledger = new ArrayList<>();
  private Map<String, Integer> accounts = new HashMap<>();

  public ServerState() {
    this.ledger = new ArrayList<>();
  }

  public void add(Operation op) {
    ledger.add(op);
    op.apply(this);
  }

  public Map<String, Integer> getAccounts() {
    return accounts;
  }
}
