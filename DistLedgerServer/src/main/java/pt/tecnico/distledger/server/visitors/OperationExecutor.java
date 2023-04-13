package pt.tecnico.distledger.server.visitors;

import pt.tecnico.distledger.common.Logger;
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

  public OperationExecutor(ServerState state) {
    this.state = state;
  }

  public void execute(Operation op) {
    op.accept(this);
  }

  @Override
  public void visit(CreateOp op) {
    Account old = this.state.getAccounts().putIfAbsent(op.getUserId(), new Account());
    if (old != null) {
      throw new AccountAlreadyExistsException(op.getUserId());
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

        // Transfer the balance
        fromAccount.setBalance(fromAccount.getBalance() - op.getAmount());
        destAccount.setBalance(destAccount.getBalance() + op.getAmount());
      }
    }

    Logger.debug(
        "Transferred " + op.getAmount() + " from " + op.getUserId() + " to " + op.getDestUserId());
  }
}
