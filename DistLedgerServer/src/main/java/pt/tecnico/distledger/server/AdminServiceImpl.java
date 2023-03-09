package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GetLedgerStateResponse;
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
      GetLedgerStateRequest request, StreamObserver<GetLedgerStateResponse> responseObserver) {
    LedgerStateGenerator generator = new LedgerStateGenerator();

    this.state.visitLedger(generator);

    GetLedgerStateResponse response =
        GetLedgerStateResponse.newBuilder().setLedgerState(generator.build()).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
