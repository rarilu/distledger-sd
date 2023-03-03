package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;
import java.util.List;
import pt.tecnico.distledger.server.domain.operation.Operation;

public class ServerState {
  private List<Operation> ledger;

  public ServerState() {
    this.ledger = new ArrayList<>();
  }

  /* TODO: Here should be declared all the server state attributes
  as well as the methods to access and interact with the state. */

}
