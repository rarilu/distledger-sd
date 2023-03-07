package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.OperationException;

public class DeleteOp extends Operation {
  public DeleteOp(String userId) {
    super(userId);
  }

  @Override
  public void apply(ServerState state) throws OperationException {
    // TODO
  }
}
