package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.NonPositiveTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NopTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;

/**
 * Represents an operation executor that executes the operations, applying their associated effects
 * to the server state.
 */
public class OperationExecutor implements OperationVisitor {
  private final ServerState state;
  ThreadLocal<VectorClock> lastTimestamp = new ThreadLocal<>();

  public OperationExecutor(ServerState state) {
    this.state = state;
  }

  public VectorClock execute(Operation op) {
    op.accept(this);
    return this.lastTimestamp.get();
  }

  @Override
  public void visit(CreateOp op) {
    Account account = new Account();

    // Safety: the account needs to be locked between the .putIfAbsent() and the .addToLedger()
    // because another operation could be executed between those two calls and access the account,
    // possibly adding it to the ledger before this operation does, which would cause the ledger to
    // be incoherent
    //
    // Liveness: it's impossible for a deadlock to occur because the account was just created, and
    // only this thread has access to it

    // noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (account) {
      Account old = this.state.getAccounts().putIfAbsent(op.getUserId(), account);
      if (old != null) {
        throw new AccountAlreadyExistsException(op.getUserId());
      }

      try {
        lastTimestamp.set(this.state.addToLedger(op));
      } catch (RuntimeException e) {
        // If the operation failed to be added to the ledger, we need to remove the account from
        // the map, otherwise it would be in an inconsistent state
        this.state.getAccounts().remove(op.getUserId());
        throw e;
      }
    }

    Logger.debug("Created account for " + op.getUserId());
  }

  @Override
  public void visit(TransferOp op) {
    // Check if the amount is positive
    if (op.getAmount() <= 0) {
      throw new NonPositiveTransferException();
    }

    final int order = op.getUserId().compareTo(op.getDestUserId());
    if (order == 0) {
      throw new NopTransferException();
    }

    // Get the accounts, and do an initial check to see if they exist
    final Account fromAccount = this.state.getAccounts().get(op.getUserId());
    if (fromAccount == null) {
      throw new UnknownAccountException(op.getUserId());
    }

    final Account destAccount = this.state.getAccounts().get(op.getDestUserId());
    if (destAccount == null) {
      throw new UnknownAccountException(op.getDestUserId());
    }

    // Safety: since both accounts are locked, inside the sync block we can check safely if they
    // exist, and if they do, if there is enough balance, and, ultimately, transfer the amount
    // without worrying about other operations accessing the accounts in the meantime
    //
    // Liveness: we lock the accounts in lexicographical order, otherwise a deadlock could occur
    // when two or more symmetric (cyclic) transfers are executed in parallel - one thread could be
    // waiting for the other to release the lock, while the other is waiting for the first to
    // release the lock
    //
    // Liveness: accounts never transfer to themselves, which is checked before the sync block, so
    // no deadlock can occur by locking the same account twice (basically a special case of the
    // previous point)
    synchronized (order > 0 ? fromAccount : destAccount) {
      synchronized (order > 0 ? destAccount : fromAccount) {
        // Check if the account has enough balance
        if (fromAccount.getBalance() < op.getAmount()) {
          throw new NotEnoughBalanceException(op.getUserId(), op.getAmount());
        }

        // No need to catch and rethrow the exception here, because we haven't made any changes to
        // the state yet, so if the operation fails to be added to the ledger, we can just let the
        // exception bubble up
        lastTimestamp.set(this.state.addToLedger(op));

        // Transfer the balance
        fromAccount.setBalance(fromAccount.getBalance() - op.getAmount());
        destAccount.setBalance(destAccount.getBalance() + op.getAmount());
      }
    }

    Logger.debug(
        "Transferred " + op.getAmount() + " from " + op.getUserId() + " to " + op.getDestUserId());
  }
}
