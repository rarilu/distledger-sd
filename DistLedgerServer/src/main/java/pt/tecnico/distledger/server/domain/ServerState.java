package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitors.OperationExecutor;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

public class ServerState {
  private final List<Operation> ledger = Collections.synchronizedList(new ArrayList<>());
  private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
  private final OperationExecutor executor;

  public ServerState() {
    this.accounts.put("broker", new Account(1000));
    this.executor = new OperationExecutor(this);
  }

  public void addToLedger(Operation op) {
    // Safety: synchronized list, it's okay to add to it without a synchronized block
    this.ledger.add(op);
  }

  public void visitLedger(OperationVisitor visitor) {
    // Safety: prevent operations from being added to the ledger while we are visiting it
    // Operations themselves are read-only, so thats not an issue
    synchronized (this.ledger) {
      this.ledger.forEach(op -> op.accept(visitor));
    }
  }

  public int getAccountBalance(String userId) {
    // Safety: if the account is deleted after the .get(), but before the .getBalance(), there is no
    // problem because the account will still have the balance before it was removed from the map
    return Optional.ofNullable(this.accounts.get(userId))
        .map(Account::getBalance)
        .orElseThrow(() -> new UnknownAccountException(userId));
  }

  public ConcurrentMap<String, Account> getAccounts() {
    return this.accounts;
  }
}
