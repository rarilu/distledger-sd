package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.utils.Logger;

public class ServerMain {
  public static void main(String[] args) throws IOException, InterruptedException {
    Logger.debug(ServerMain.class.getSimpleName());

    // Check arguments
    if (args.length != 2) {
      Logger.error("Argument(s) missing!");
      Logger.error("Usage: mvn exec:java -Dexec.args=<port> <qual>");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final String qualifier = args[1];

    // Init server state
    final ServerState state = new ServerState();

    // Init active flag
    final AtomicBoolean active = new AtomicBoolean(true);

    // Init services
    final BindableService userService = new UserServiceImpl(state, active);
    final BindableService adminService = new AdminServiceImpl(state, active);

    // Launch server
    final Server server =
        ServerBuilder.forPort(port).addService(userService).addService(adminService).build();
    server.start();
    System.out.println("Server started, listening on " + port);

    // Wait until server is terminated
    while (!server.isTerminated()) {
      try {
        server.awaitTermination();
      } catch (InterruptedException e) {
        // Shutdown gracefully on interrupt.
        System.out.println("Server interrupted, shutting down");
        server.shutdown();
      }
    }
  }
}
