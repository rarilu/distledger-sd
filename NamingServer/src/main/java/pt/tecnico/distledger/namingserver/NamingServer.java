package pt.tecnico.distledger.namingserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;

/** Main class for the Naming server. */
public class NamingServer {
  private static final int PORT = 5001;

  /** Main method. */
  public static void main(String[] args) throws IOException {
    Logger.debug(NamingServer.class.getSimpleName());

    // Init naming server state
    final NamingServerState state = new NamingServerState();

    // Init service
    final NamingServiceImpl namingService = new NamingServiceImpl(state);

    // Launch server
    final Server server = ServerBuilder.forPort(PORT).addService(namingService).build();
    namingService.setServer(server); // Called so that the service can shutdown the server
    server.start();
    System.out.println("Naming Server started, listening on " + PORT);

    // Wait until server is terminated
    while (!server.isTerminated()) {
      try {
        server.awaitTermination();
      } catch (InterruptedException e) {
        // Shutdown gracefully on interrupt
        System.out.println("Naming Server interrupted, shutting down");
        server.shutdown();
      }
    }
  }
}
