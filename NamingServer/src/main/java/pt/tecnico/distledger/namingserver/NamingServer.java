package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.grpc.NamingServiceImpl;

/** Main class for the Naming server. */
public class NamingServer {
  private static final int PORT = 5001;

  /** Main method. */
  public static void main(String[] args) throws IOException, InterruptedException {
    Logger.debug(NamingServer.class.getSimpleName());

    // Init naming server state
    final NamingServerState state = new NamingServerState();

    // Init service
    final BindableService namingService = new NamingServiceImpl(state);

    // Launch server
    final Server server = ServerBuilder.forPort(PORT).addService(namingService).build();
    server.start();
    System.out.println("Naming Server started, listening on " + PORT);

    // Wait for user input to shut down server
    System.out.println("Press enter to shutdown");
    System.in.read();

    // Wait until server is terminated
    server.shutdown();
    server.awaitTermination();

    Logger.debug("Naming Server terminated");
  }
}
