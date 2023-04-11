package pt.tecnico.distledger.namingserver.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteResponse;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupResponse;
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
  private static final String LOOKUP_FAILED = "Lookup failed: ";

  private final NamingServerState state;

  public NamingServiceImpl(NamingServerState state) {
    this.state = state;
  }

  @Override
  public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
    Logger.debug("Received Register request:");
    Logger.debug(request + "\n");

    try {
      int id = this.state.registerServer(request.getService(), request.getQualifier(), request.getTarget());
      responseObserver.onNext(RegisterResponse.newBuilder().setAssignedId(id).build());
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
    Logger.debug("Received Delete request:");
    Logger.debug(request + "\n");

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

  @Override
  public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
    Logger.debug("Received Lookup request:");
    Logger.debug(request + "\n");

    try {
      // Lookup the target servers with the requested characteristics.
      List<String> targets;
      if (request.getQualifier().isEmpty()) {
        targets = this.state.lookup(request.getService());
      } else {
        targets = this.state.lookup(request.getService(), request.getQualifier());
      }

      responseObserver.onNext(LookupResponse.newBuilder().addAllTargets(targets).build());
      responseObserver.onCompleted();
    } catch (RuntimeException e) {
      Logger.debug(LOOKUP_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
