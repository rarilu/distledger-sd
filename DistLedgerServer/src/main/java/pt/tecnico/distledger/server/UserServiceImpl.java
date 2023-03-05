package pt.tecnico.distledger.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.exceptions.OperationException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
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
      state.add(new CreateOp(request.getUserId()));
      responseObserver.onNext(CreateAccountResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (OperationException e) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withCause(e).withDescription(e.getMessage()).asException());
    }
  }
}
