package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {
  private ServerState state;
  private AtomicBoolean active;

  public AdminServiceImpl(ServerState state, AtomicBoolean active) {
    this.state = state;
    this.active = active;
  }

  @Override
  public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
    this.active.set(true);
    responseObserver.onNext(ActivateResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void deactivate(
      DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
    this.active.set(false);
    responseObserver.onNext(DeactivateResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void getLedgerState(
      getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
    LedgerStateGenerator generator = new LedgerStateGenerator();

    this.state.visitLedger(generator);

    getLedgerStateResponse response =
        getLedgerStateResponse.newBuilder().setLedgerState(generator.build()).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
