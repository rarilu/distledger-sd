package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.domain.exceptions.OutdatedStateException;
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents the current state of the server. */
public class ServerState {
  private final int id;
  private final List<Operation> ledger = Collections.synchronizedList(new ArrayList<>());
  private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
  private final VectorClock valueTimestamp = new VectorClock();

  public ServerState(int id) {
    this.id = id;
    this.accounts.put("broker", new Account(1000));
  }

  /** Register a given operation in the ledger. */
  public void addToLedger(Operation op) {
    synchronized (this.valueTimestamp) {
      this.valueTimestamp.merge(op.getPrevTS());
    }

    // Safety: synchronized list, it's okay to add to it without a synchronized
    // block
    this.ledger.add(op);
  }

  /** Visit all operations in the ledger, using the specified visitor. */
  public void visitLedger(OperationVisitor visitor) {
    // Safety: prevent operations from being added to the ledger while we are
    // visiting it
    // Operations themselves are read-only, so that's not an issue
    synchronized (this.ledger) {
      this.ledger.forEach(op -> op.accept(visitor));
    }
  }

  /**
   * Returns the balance of the account with the given User ID.
   *
   * <p>Safety: prevTS must not be written to during execution of this method
   */
  public int getAccountBalance(String userId, VectorClock prevTS) {
    Optional<Account> account;

    synchronized (this.valueTimestamp) {
      switch (VectorClock.compare(prevTS, this.valueTimestamp)) {
        case BEFORE:
        case EQUAL:
          account = Optional.ofNullable(this.accounts.get(userId));
          break;
        default:
          throw new OutdatedStateException(prevTS, this.valueTimestamp);
      }
    }

    return account.map(Account::getBalance).orElseThrow(() -> new UnknownAccountException(userId));
  }

  /** Returns the current list of accounts. */
  public ConcurrentMap<String, Account> getAccounts() {
    return this.accounts;
  }

  /** Returns the ID of the server. */
  public int getId() {
    return this.id;
  }
}
