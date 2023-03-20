package pt.tecnico.distledger.namingserver;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterResponse;
import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;

/** Implements the Admin service, handling gRPC requests. */
public class NamingServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {
  private static final String REGISTER_FAILED = "Register failed: ";

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
}
