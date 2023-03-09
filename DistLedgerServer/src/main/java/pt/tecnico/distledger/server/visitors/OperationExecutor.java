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
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.utils.Logger;

public class OperationExecutor implements OperationVisitor {
  private ServerState state;

  public OperationExecutor(ServerState state) {
    this.state = state;
  }

  @Override
  public void visit(CreateOp op) {
    if (this.state.getAccounts().containsKey(op.getUserId())) {
      throw new AccountAlreadyExistsException(op.getUserId());
    }

    this.state.getAccounts().put(op.getUserId(), new Account());
    Logger.debug("Created account for " + op.getUserId());
  }

  @Override
  public void visit(DeleteOp op) {
    if (op.getUserId().equals("broker")) {
      throw new ProtectedAccountException(op.getUserId());
    }

    if (!state.getAccounts().containsKey(op.getUserId())) {
      throw new UnknownAccountException(op.getUserId());
    }

    final int balance = state.getAccounts().get(op.getUserId()).getBalance();
    if (balance > 0) {
      throw new NonEmptyAccountException(op.getUserId(), balance);
    }

    state.getAccounts().remove(op.getUserId());
    Logger.debug("Deleted account of " + op.getUserId());
  }

  @Override
  public void visit(TransferOp op) {
    // Check if the amount is positive.
    if (op.getAmount() <= 0) {
      throw new NonPositiveTransferException();
    }

    // Check if the accounts are the same.
    if (op.getUserId().equals(op.getDestUserId())) {
      throw new NopTransferException();
    }

    // Get the accounts.
    Account fromAccount = state.getAccounts().get(op.getUserId());
    Account destAccount = state.getAccounts().get(op.getDestUserId());

    // Check if the accounts exist.
    if (fromAccount == null) {
      throw new UnknownAccountException(op.getUserId());
    }

    if (destAccount == null) {
      throw new UnknownAccountException(op.getDestUserId());
    }

    // Check if the account has enough money.
    if (fromAccount.getBalance() < op.getAmount()) {
      throw new NotEnoughBalanceException(op.getUserId(), op.getAmount());
    }

    // Transfer the money.
    fromAccount.setBalance(fromAccount.getBalance() - op.getAmount());
    destAccount.setBalance(destAccount.getBalance() + op.getAmount());

    Logger.debug(
        "Transferred " + op.getAmount() + " from " + op.getUserId() + " to " + op.getDestUserId());
  }
}
