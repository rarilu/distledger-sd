package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.contract.namingserver.NamingServiceGrpc;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;

/** Implements the Admin service, handling gRPC requests. */
public class NamingServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {
  private final NamingServerState state;

  public NamingServiceImpl(NamingServerState state) {
    this.state = state;
  }
}
