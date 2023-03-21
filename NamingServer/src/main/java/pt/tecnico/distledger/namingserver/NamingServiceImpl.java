package pt.tecnico.distledger.namingserver;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteResponse;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterResponse;
import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Implements the Admin service, handling gRPC requests. */
public class NamingServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {
  private static final String REGISTER_FAILED = "Register failed: ";
  private static final String DELETE_FAILED = "Delete failed: ";

  private final NamingServerState state;

  public NamingServiceImpl(NamingServerState state) {
    this.state = state;
  }

  @Override
  public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
    try {
      this.state.registerServer(request.getService(), request.getQualifier(), request.getTarget());
      responseObserver.onNext(RegisterResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (DuplicateServerEntryException e) {
      Logger.debug(REGISTER_FAILED + e.getMessage());
      responseObserver.onError(
          Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug(REGISTER_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
    try {
      this.state.deleteServer(request.getService(), request.getTarget());
      responseObserver.onNext(DeleteResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (ServerEntryNotFoundException e) {
      Logger.debug(DELETE_FAILED + e.getMessage());
      responseObserver.onError(
          Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      Logger.debug(DELETE_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
