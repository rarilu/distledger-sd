package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.common.domain.VectorClock.Order;
import pt.tecnico.distledger.server.domain.exceptions.OutdatedStateException;
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitors.OperationExecutor;
import pt.tecnico.distledger.server.visitors.OperationVisitor;

/** Represents the current state of the server. */
public class ServerState {
  private final int id;
  private final List<Operation> ledger = Collections.synchronizedList(new ArrayList<>());
  private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
  private final VectorClock valueTimeStamp = new VectorClock();
  private final VectorClock replicaTimeStamp = new VectorClock();
  private AtomicInteger firstUnstable = new AtomicInteger(0);
  private OperationExecutor executor = new OperationExecutor(this);

  public ServerState(int id) {
    this.id = id;
    this.accounts.put("broker", new Account(1000));
  }

  /**
   * Generates a new unique timestamp for an operation received in this replica from a client.
   * Increments the timestamp of this replica and merges it with the timestamp of the client.
   *
   * @param prevTimeStamp the timestamp of the client when it sent the operation
   * @return the new timestamp.
   */
  public VectorClock generateTimeStamp(VectorClock prevTimeStamp) {
    VectorClock timeStamp;

    // Safety: the timestamp is locked to avoid concurrent increments
    synchronized (this.replicaTimeStamp) {
      this.replicaTimeStamp.increment(this.id);
      timeStamp = new VectorClock(this.replicaTimeStamp);
      timeStamp.mergeSingle(this.replicaTimeStamp, this.id);
    }

    return timeStamp;
  }

  /**
   * Register a given operation in the ledger. If the operation can be executed immediately, it will
   * immediatelly be stabilized. Otherwise, it will be added to the ledger and stabilized later.
   *
   * @param rejectDuplicates if true, will reject if TS <= replicaTS
   * @return true if it was immediately stabilized, false otherwise.
   */
  public boolean addToLedger(Operation op, boolean rejectDuplicates) {
    // Check if the operation is a duplicate
    if (rejectDuplicates && this.isDuplicate(op)) {
      return false;
    }

    // Merge the replica timestamp with the operation timestamp
    synchronized (this.replicaTimeStamp) {
      this.replicaTimeStamp.merge(op.getTimeStamp());
    }

    // Check if the operation can be immediately executed
    if (this.canStabilize(op)) {
      // If it can, mark it as stable and execute it
      op.setStable();
      this.execute(op);

      // Safety: the ledger must be locked to avoid concurrent modifications
      // If the firstUnstable index were to be incremented outside of the synchronized block,
      // it would be possible for another thread to add an operation to the ledger and it would
      // be within the slice considered stable, which could cause some unexpected issues.
      synchronized (this.ledger) {
        // Add it to the ledger, before the first unstable operation
        int index = this.firstUnstable.getAndIncrement();

        if (index < this.ledger.size()) {
          // Swap the operation with the first unstable operation, if it's not already there
          this.ledger.add(this.ledger.get(index));
          this.ledger.set(index, op);
        } else {
          this.ledger.add(op);
        }
      }

      return true;
    } else {
      // If it can't, just add it to the ledger and wait for stabilization
      // Safety: synchronized list, it's okay to add to it without a synchronized block.
      this.ledger.add(op);
      return false;
    }
  }

  /** Executes all operations in the ledger that can be executed. */
  public void stabilize() {
    boolean isStable;

    do {
      isStable = true;

      for (int i = this.firstUnstable.get(), last = this.ledger.size(); i < last; i++) {
        Operation op = this.ledger.get(i);

        // Safety: we first sync on the operation, and check if it's stable. If it was already
        // marked as stable, we skip it since it means another thread is already executing it.
        // If it's not stable, we can set it as stable and leave the synchronized block, since
        // other threads will see it as stable and skip it.
        synchronized (op) {
          // If the operation is already stable or can't be stabilized, skip it
          if (op.isStable() || !this.canStabilize(op)) {
            continue;
          }

          op.setStable();
        }

        // Swap the operation with the first unstable operation, if it's not already there
        int swapIndex = firstUnstable.getAndIncrement();
        if (swapIndex != i) {
          synchronized (this.ledger) {
            Operation swap = this.ledger.get(swapIndex);
            this.ledger.set(swapIndex, op);
            this.ledger.set(i, swap);
          }
        }

        this.execute(op);

        // Unset the isStable flag, so that stabilize searches for new operations to stabilize
        isStable = false;
      }
    } while (!isStable);
  }

  /**
   * Visit all operations in the ledger, using the specified visitor.
   *
   * @param visitor the visitor for each operation to accept.
   * @param startAtIndex the index to start visiting from.
   * @return the index of the last stable operation visited, if any.
   */
  public Optional<Integer> visitLedger(OperationVisitor visitor, int startAtIndex) {
    // Safety: prevent operations from being added to the ledger while we are
    // visiting it
    // Operations themselves are thread-safe, so we don't need to lock them: the only mutable
    // operation state is atomic.
    synchronized (this.ledger) {
      Optional<Integer> lastStable = Optional.empty();
      boolean foundUnstable = false;

      for (int i = startAtIndex; i < this.ledger.size(); i++) {
        // Safety: no need to lock the ledger, its a synchronized list
        Operation op = this.ledger.get(i);

        // If this operation is stable, mark it as the last stable operation
        if (op.isStable()) {
          // As soon as we find an unstable operation, we should not count any more stable
          // operations after it - those operations are still being ordered.
          if (!foundUnstable) {
            lastStable = Optional.of(i);
          }
        } else {
          foundUnstable = true;
        }

        op.accept(visitor);
      }

      return lastStable;
    }
  }

  public void visitLedger(OperationVisitor visitor) {
    this.visitLedger(visitor, 0);
  }

  /**
   * Returns the balance of the account with the given User ID, stamped with the current value
   * timestamp.
   *
   * <p>Safety: prevTS must not be written to during execution of this method
   */
  public Stamped<Integer> getAccountBalance(String userId, VectorClock prevTimeStamp) {
    Optional<Account> account;
    VectorClock timeStamp;

    synchronized (this.valueTimeStamp) {
      switch (VectorClock.compare(prevTimeStamp, this.valueTimeStamp)) {
        case BEFORE:
        case EQUAL:
          account = Optional.ofNullable(this.accounts.get(userId));
          timeStamp = new VectorClock(this.valueTimeStamp);
          break;
        default:
          throw new OutdatedStateException(prevTimeStamp, this.valueTimeStamp);
      }
    }

    return account
        .map(acc -> new Stamped<>(acc.getBalance(), timeStamp))
        .orElseThrow(() -> new UnknownAccountException(userId));
  }

  /** Returns the current list of accounts. */
  public ConcurrentMap<String, Account> getAccounts() {
    return this.accounts;
  }

  /** Returns the ID of the server. */
  public int getId() {
    return this.id;
  }

  /** Executes an operation. */
  private void execute(Operation op) {
    // Execute the operation, possibly concurrently with other threads
    try {
      op.accept(this.executor);

      // Merge the operation's timestamp with the current value timestamp
      synchronized (this.valueTimeStamp) {
        this.valueTimeStamp.merge(op.getTimeStamp());
      }
    } catch (RuntimeException e) {
      // If the operation fails, mark it as failed and log the error
      op.setFailed();
      System.err.println("Operation failed: " + e.getMessage());
    }
  }

  /** Checks if an operation can be stabilized. */
  private boolean canStabilize(Operation op) {
    VectorClock.Order order;
    synchronized (this.valueTimeStamp) {
      order = VectorClock.compare(op.getPrevTimeStamp(), this.valueTimeStamp);
    }

    return order == Order.BEFORE || order == Order.EQUAL;
  }

  /** Checks if an operation is a duplicate. */
  private boolean isDuplicate(Operation op) {
    VectorClock.Order order;
    synchronized (this.replicaTimeStamp) {
      order = VectorClock.compare(op.getTimeStamp(), this.replicaTimeStamp);
    }

    return order == Order.BEFORE || order == Order.EQUAL;
  }
}
