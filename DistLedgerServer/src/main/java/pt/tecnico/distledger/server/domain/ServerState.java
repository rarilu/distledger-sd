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
  public VectorClock addToLedger(Operation op) {
    VectorClock timestamp;

    synchronized (this.valueTimestamp) {
      this.valueTimestamp.merge(op.getPrevTimestamp());
      timestamp = new VectorClock(this.valueTimestamp);
      timestamp.mergeSingle(op.getPrevTimestamp(), this.id);
    }

    // Safety: synchronized list, it's okay to add to it without a synchronized
    // block
    this.ledger.add(op);
    return timestamp;
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
  public Stamped<Integer> getAccountBalance(String userId, VectorClock prevTimestamp) {
    Optional<Account> account;
    VectorClock timestamp;

    synchronized (this.valueTimestamp) {
      switch (VectorClock.compare(prevTimestamp, this.valueTimestamp)) {
        case BEFORE:
        case EQUAL:
          account = Optional.ofNullable(this.accounts.get(userId));
          timestamp = new VectorClock(this.valueTimestamp);
          break;
        default:
          throw new OutdatedStateException(prevTimestamp, this.valueTimestamp);
      }
    }

    if (account.isPresent()) {
      return new Stamped<>(account.get().getBalance(), timestamp);
    } else {
      throw new UnknownAccountException(userId);
    }
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
