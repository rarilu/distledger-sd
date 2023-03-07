package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.OperationException;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.utils.Logger;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
  private ServerState state;

  public UserServiceImpl(ServerState state) {
    this.state = state;
  }

  @Override
  public void createAccount(
      CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    try {
      this.state.registerOperation(new CreateOp(request.getUserId()));
      responseObserver.onNext(CreateAccountResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (OperationException e) {
      Logger.debug("Create account failed: " + e.getMessage());
      responseObserver.onError(e.getGrpcStatus().asRuntimeException());
    }
  }

  @Override
  public void transferTo(
      TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
    try {
      state.registerOperation(
          new TransferOp(request.getAccountFrom(), request.getAccountTo(), request.getAmount()));
      responseObserver.onNext(TransferToResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (OperationException e) {
      Logger.debug("Transfer failed: " + e.getMessage());
      responseObserver.onError(e.getGrpcStatus().asRuntimeException());
    }
  }
}
