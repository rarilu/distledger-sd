package pt.tecnico.distledger.server.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.domain.VectorClock;
import pt.tecnico.distledger.common.grpc.ProtoUtils;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.Stamped;
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.NonPositiveTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NopTransferException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.grpc.exceptions.FailedPropagationException;
import pt.tecnico.distledger.server.visitors.OperationExecutor;

/** Implements the User service, handling gRPC requests. */
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
  private static final String CREATE_ACCOUNT_FAILED = "Create account failed: ";
  private static final String TRANSFER_FAILED = "Transfer failed: ";
  private static final String BALANCE_FAILED = "Balance failed: ";

  private final ServerState state;
  private final AtomicBoolean active;
  private final OperationExecutor executor;

  /**
   * Creates a new UserServiceImpl, with an associated {@link OperationExecutor}.
   *
   * @param state The server state
   * @param active This server's active flag
   */
  public UserServiceImpl(ServerState state, AtomicBoolean active) {
    this.state = state;
    this.active = active;
    this.executor = new OperationExecutor(state);
  }

  @Override
  public void createAccount(
      CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    Logger.debug("Received CreateAccount request:");
    Logger.debug(request + "\n");

    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      VectorClock valueTimeStamp =
          this.executor.execute(
              new CreateOp(
                  request.getUserId(),
                  ProtoUtils.fromProto(request.getPrevTS()),
                  new VectorClock()));
      responseObserver.onNext(
          CreateAccountResponse.newBuilder()
              .setValueTS(ProtoUtils.toProto(valueTimeStamp))
              .build());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug(CREATE_ACCOUNT_FAILED + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (AccountAlreadyExistsException e) {
      Logger.debug(CREATE_ACCOUNT_FAILED + e.getMessage());
      responseObserver.onError(
          Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
    } catch (FailedPropagationException e) {
      Logger.debug(CREATE_ACCOUNT_FAILED + e.getMessage());
      responseObserver.onError(Status.ABORTED.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug(CREATE_ACCOUNT_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void transferTo(
      TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
    Logger.debug("Received TransferTo request:");
    Logger.debug(request + "\n");

    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      VectorClock valueTimeStamp =
          this.executor.execute(
              new TransferOp(
                  request.getAccountFrom(),
                  request.getAccountTo(),
                  request.getAmount(),
                  ProtoUtils.fromProto(request.getPrevTS()),
                  new VectorClock()));
      responseObserver.onNext(
          TransferToResponse.newBuilder().setValueTS(ProtoUtils.toProto(valueTimeStamp)).build());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug(TRANSFER_FAILED + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (NonPositiveTransferException | NopTransferException e) {
      Logger.debug(TRANSFER_FAILED + e.getMessage());
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnknownAccountException e) {
      Logger.debug(TRANSFER_FAILED + e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (NotEnoughBalanceException e) {
      Logger.debug(TRANSFER_FAILED + e.getMessage());
      responseObserver.onError(
          Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
    } catch (FailedPropagationException e) {
      Logger.debug(TRANSFER_FAILED + e.getMessage());
      responseObserver.onError(Status.ABORTED.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug(TRANSFER_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
    Logger.debug("Received Balance request:");
    Logger.debug(request + "\n");

    try {
      if (!active.get()) {
        throw new ServerUnavailableException();
      }
      final Stamped<Integer> balance =
          this.state.getAccountBalance(
              request.getUserId(), ProtoUtils.fromProto(request.getPrevTS()));
      responseObserver.onNext(
          BalanceResponse.newBuilder()
              .setValue(balance.value())
              .setValueTS(ProtoUtils.toProto(balance.timeStamp()))
              .build());
      responseObserver.onCompleted();
    } catch (ServerUnavailableException e) {
      Logger.debug(BALANCE_FAILED + e.getMessage());
      responseObserver.onError(
          Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnknownAccountException e) {
      Logger.debug(BALANCE_FAILED + e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug(BALANCE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
