package pt.tecnico.distledger.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.utils.Logger;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
  private ServerState state;
  private AtomicBoolean active;

  public UserServiceImpl(ServerState state, AtomicBoolean active) {
    this.state = state;
    this.active = active;
  }

  @Override
  public void createAccount(
      CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      this.state.registerOperation(new CreateOp(request.getUserId()));
      responseObserver.onNext(CreateAccountResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug("Create account failed: " + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (AccountAlreadyExistsException e) {
      Logger.debug("Create account failed: " + e.getMessage());
      responseObserver.onError(
          Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug("Create account failed: " + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void deleteAccount(
      DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      this.state.registerOperation(new DeleteOp(request.getUserId()));
      responseObserver.onNext(DeleteAccountResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug("Delete account failed: " + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (ProtectedAccountException e) {
      Logger.debug("Delete account failed: " + e.getMessage());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnknownAccountException e) {
      Logger.debug("Delete account failed: " + e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (NonEmptyAccountException e) {
      Logger.debug("Delete account failed: " + e.getMessage());
      responseObserver.onError(
          Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug("Delete account failed: " + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void transferTo(
      TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      this.state.registerOperation(
          new TransferOp(request.getAccountFrom(), request.getAccountTo(), request.getAmount()));
      responseObserver.onNext(TransferToResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug("Transfer failed: " + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (NonPositiveTransferException | NopTransferException e) {
      Logger.debug("Transfer failed: " + e.getMessage());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnknownAccountException e) {
      Logger.debug("Transfer failed: " + e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (NotEnoughBalanceException e) {
      Logger.debug("Transfer failed: " + e.getMessage());
      responseObserver.onError(
          Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug("Transfer failed: " + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      final int balance = this.state.getAccountBalance(request.getUserId());
      responseObserver.onNext(BalanceResponse.newBuilder().setValue(balance).build());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug("Balance failed: " + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnknownAccountException e) {
      Logger.debug("Balance failed: " + e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug("Balance failed: " + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
