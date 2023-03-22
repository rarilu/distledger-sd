package pt.tecnico.distledger.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.NamingService;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.visitors.DummyOperationExecutor;
import pt.tecnico.distledger.server.visitors.OperationExecutor;
import pt.tecnico.distledger.server.visitors.StandardOperationExecutor;

/** Main class for the DistLedger server. */
public class ServerMain {
  private static final String SERVICE_NAME = "DistLedger";
  private static final String PRIMARY_QUALIFIER = "A";

  /** Main method. */
  public static void main(String[] args) throws IOException {
    Logger.debug(ServerMain.class.getSimpleName());

    // Check arguments
    if (args.length != 2) {
      Logger.error("Argument(s) missing!");
      Logger.error("Usage: mvn exec:java -Dexec.args=\"<port> <qual>\"");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final String qualifier = args[1];

    // Init server state
    final ServerState state = new ServerState();

    // Init active flag
    final AtomicBoolean active = new AtomicBoolean(true);

    // Init operation executor to use when applying operations to the server state:
    //   - if this is the primary server (= has appropriate qualifier), use the standard executor
    //   - otherwise, use the dummy executor (which only throws an exception for all operations)
    final boolean isPrimary = Objects.equals(qualifier, PRIMARY_QUALIFIER);
    final OperationExecutor executor =
        isPrimary ? new StandardOperationExecutor(state) : new DummyOperationExecutor();

    // Init services
    final UserServiceImpl userService = new UserServiceImpl(state, active, executor);
    final AdminServiceImpl adminService = new AdminServiceImpl(state, active);

    // Launch server
    final Server server =
        ServerBuilder.forPort(port).addService(userService).addService(adminService).build();
    adminService.setServer(server);
    server.start();
    System.out.println("Server started, listening on " + port);

    // Connect to naming server and register this server
    try (NamingService namingService = new NamingService()) {
      try {
        final String address = InetAddress.getLocalHost().getHostAddress().toString();
        Logger.debug("Registering server at " + address + ":" + port);
        namingService.register(SERVICE_NAME, qualifier, address + ":" + port);
      } catch (RuntimeException e) {
        Logger.error("Failed to register server: " + e.getMessage());
        server.shutdown();
      }

      // Wait until server is terminated
      while (!server.isTerminated()) {
        try {
          server.awaitTermination();
        } catch (InterruptedException e) {
          // Shutdown gracefully on interrupt
          System.out.println("Server interrupted, shutting down");
          server.shutdown();
        }
      }

      // Unregister server
      namingService.delete(SERVICE_NAME, qualifier);
    }
  }
}
