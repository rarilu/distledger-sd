package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.NonEmptyAccountException;
import pt.tecnico.distledger.server.domain.exceptions.NonPositiveTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NopTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ProtectedAccountException;
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.utils.Logger;

/**
 * Represents an operation visitor that executes the operations, applying their associated effects
 * to the server state.
 */
public class OperationExecutor implements OperationVisitor {
  private final ServerState state;

  public OperationExecutor(ServerState state) {
    this.state = state;
  }

  public void execute(Operation op) {
    op.accept(this);
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

      this.state.addToLedger(op);
    }

    Logger.debug("Created account for " + op.getUserId());
  }

  @Override
  public void visit(DeleteOp op) {
    if (op.getUserId().equals("broker")) {
      throw new ProtectedAccountException(op.getUserId());
    }

    Account account = this.state.getAccounts().get(op.getUserId());
    if (account == null) {
      throw new UnknownAccountException(op.getUserId());
    }

    // Safety: the account is only deleted when we are sure that no other operation is accessing
    //
    // Safety: the operation is added to the ledger before the account is removed from the map,
    // because otherwise a CreateOp could be executed in the meantime and add an account with the
    // same ID to the ledger, before this operation is added, which would cause the ledger to be
    // incoherent
    //
    // Liveness: only one synchronized block is needed, so this operation can never cause deadlocks

    // noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (account) {
      // Now make sure that it wasn't deleted in the meantime
      if (!this.state.getAccounts().containsKey(op.getUserId())) {
        throw new UnknownAccountException(op.getUserId());
      }

      // Check if it has balance
      final int balance = state.getAccounts().get(op.getUserId()).getBalance();
      if (balance > 0) {
        throw new NonEmptyAccountException(op.getUserId(), balance);
      }

      this.state.addToLedger(op);

      // Now we can safely delete it since no other operation can access it
      this.state.getAccounts().remove(op.getUserId());
    }

    Logger.debug("Deleted account of " + op.getUserId());
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
        // The accounts may have been deleted in the meantime, so we need to check again, now that
        // they are locked
        if (!this.state.getAccounts().containsKey(op.getUserId())) {
          throw new UnknownAccountException(op.getUserId());
        }

        if (!this.state.getAccounts().containsKey(op.getDestUserId())) {
          throw new UnknownAccountException(op.getDestUserId());
        }

        // Check if the account has enough balance
        if (fromAccount.getBalance() < op.getAmount()) {
          throw new NotEnoughBalanceException(op.getUserId(), op.getAmount());
        }

        // Transfer the balance
        fromAccount.setBalance(fromAccount.getBalance() - op.getAmount());
        destAccount.setBalance(destAccount.getBalance() + op.getAmount());

        // Add the operation to the ledger
        this.state.addToLedger(op);
      }
    }

    Logger.debug(
        "Transferred " + op.getAmount() + " from " + op.getUserId() + " to " + op.getDestUserId());
  }
}
