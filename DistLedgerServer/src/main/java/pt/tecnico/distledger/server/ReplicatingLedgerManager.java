package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.visitors.LedgerStateGenerator;

/**
 * Implementation of LedgerManager that first replicates the operation to secondary servers and only
 * then modifies the server state.
 */
public class ReplicatingLedgerManager implements LedgerManager {
  private static final String SECONDARY_QUALIFIER = "B";

  private final CrossServerService crossServerService;
  private final ServerState state;

  public ReplicatingLedgerManager(CrossServerService crossServerService, ServerState state) {
    this.crossServerService = crossServerService;
    this.state = state;
  }

  @Override
  public void addToLedger(Operation operation) {
    // First replicate the operation to secondary servers
    LedgerStateGenerator generator = new LedgerStateGenerator();
    operation.accept(generator);

    synchronized (this) {
      // propagateState() may throw a FailedPropagationException, and the caller should take that
      // into account
      this.crossServerService.propagateState(SECONDARY_QUALIFIER, generator);

      // Only then modify the server state
      this.state.addToLedger(operation);
    }
  }
}
