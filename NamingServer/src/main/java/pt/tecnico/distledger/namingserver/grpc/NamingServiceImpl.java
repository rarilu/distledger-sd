package pt.tecnico.distledger.namingserver.grpc;

import io.grpc.Server;
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
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ShutdownRequest;
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ShutdownResponse;
import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException;
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException;

/** Implements the Admin service, handling gRPC requests. */
public class NamingServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {
  private static final String REGISTER_FAILED = "Register failed: ";
  private static final String DELETE_FAILED = "Delete failed: ";
  private static final String LOOKUP_FAILED = "Lookup failed: ";
  private static final String SHUTDOWN_FAILED = "Shutdown failed: ";

  private Server server;
  private final NamingServerState state;

  public NamingServiceImpl(NamingServerState state) {
    this.server = null;
    this.state = state;
  }

  /** Sets the server using this service. Must be called before the shutdown method is called. */
  public void setServer(Server server) {
    this.server = server;
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

  @Override
  public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
    try {
      // Lookup the target servers with the requested characteristics.
      List<String> targets;
      if (request.getQualifier() == null || request.getQualifier().isEmpty()) {
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

  @Override
  public void shutdown(ShutdownRequest request, StreamObserver<ShutdownResponse> responseObserver) {
    if (this.server == null) {
      Logger.debug(SHUTDOWN_FAILED + "Server cannot be shutdown");
      responseObserver.onError(
          Status.UNIMPLEMENTED.withDescription("Server cannot be shutdown").asRuntimeException());
      return;
    }

    try {
      responseObserver.onNext(ShutdownResponse.getDefaultInstance());
      responseObserver.onCompleted();
      this.server.shutdown();
    } catch (RuntimeException e) {
      Logger.debug(SHUTDOWN_FAILED + e.getMessage());
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
