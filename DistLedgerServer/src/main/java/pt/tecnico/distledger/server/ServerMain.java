package pt.tecnico.distledger.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.NamingService;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;
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
    if (args.length != 2 && args.length != 3) {
      Logger.error("Argument(s) missing!");
      Logger.error("Usage: mvn exec:java -Dexec.args=\"<port> <qual> [<naming_server_target>]\"");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final String qualifier = args[1];
    // accepts naming server target as an optional argument
    // if not provided, uses the well-known target
    Optional<String> namingServerTarget = Arrays.stream(args).skip(2).findFirst();

    // Init server state
    final ServerState state = new ServerState();

    // Init active flag
    final AtomicBoolean active = new AtomicBoolean(true);

    // Check if this server is the primary server and initialize the operation executor used by the
    // user service to execute received operations
    final boolean isPrimary = Objects.equals(qualifier, PRIMARY_QUALIFIER);
    OperationExecutor executor;
    if (isPrimary) {
      // TODO: when the replicating ledger manager is finished this should be replaced by it
      final LedgerManager ledgerManager = new DirectLedgerManager(state);
      executor = new StandardOperationExecutor(state, ledgerManager);
    } else {
      // Use a dummy executor which only throws an exception for any operation it receives
      executor = new DummyOperationExecutor();
    }

    // Init services
    final UserServiceImpl userService = new UserServiceImpl(state, active, executor);
    final AdminServiceImpl adminService = new AdminServiceImpl(state, active);

    // Launch server
    final Server server =
        ServerBuilder.forPort(port).addService(userService).addService(adminService).build();
    adminService.setServer(server); // Called so that the service can shutdown the server
    server.start();
    System.out.println("Server started, listening on " + port);

    final String target = InetAddress.getLocalHost().getHostAddress().toString() + ":" + port;

    // Connect to naming server and register this server
    try (final NamingService namingService =
        namingServerTarget.map(NamingService::new).orElseGet(NamingService::new)) {
      try {
        Logger.debug("Registering server at " + target);
        namingService.register(SERVICE_NAME, qualifier, target);
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
      namingService.delete(SERVICE_NAME, target);
    }
  }
}
