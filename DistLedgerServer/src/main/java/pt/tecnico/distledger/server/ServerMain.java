package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.DistLedgerCrossServerServiceImpl;
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
  public static void main(String[] args) throws IOException, InterruptedException {
    Logger.debug(ServerMain.class.getSimpleName());

    // Check arguments
    if (args.length != 2 && args.length != 3) {
      Logger.error("Argument(s) missing!");
      Logger.error("Usage: mvn exec:java -Dexec.args=\"<port> <qual> [<naming_server_target>]\"");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final String qualifier = args[1];

    // Accepts naming server target as an optional argument
    // If not provided, uses the well-known target
    Optional<String> namingServerTarget = Arrays.stream(args).skip(2).findFirst();

    // Connect to the naming server
    try (final NamingService namingService =
        namingServerTarget.map(NamingService::new).orElseGet(NamingService::new)) {

      // Init server state
      final ServerState state = new ServerState();

      // Init active flag
      final AtomicBoolean active = new AtomicBoolean(true);

      // Check if the server is a primary server, and init the operation executor accordingly
      // If the server is a secondary server,
      final boolean isPrimary = Objects.equals(qualifier, PRIMARY_QUALIFIER);
      OperationExecutor executor;
      if (isPrimary) {
        // Use a standard operation executor which replicates operations to the other servers
        final CrossServerService crossServerService = new CrossServerService(namingService);
        final LedgerManager ledgerManager = new ReplicatingLedgerManager(crossServerService, state);
        executor = new StandardOperationExecutor(state, ledgerManager);
      } else {
        // Use a dummy executor which only throws an exception for any operation it receives
        executor = new DummyOperationExecutor();
      }

      // Init services
      // Currently the cross-server service implementation is not used by the primary server
      // but there is no harm in initializing it anyway - it will be useful in the future
      final BindableService userService = new UserServiceImpl(state, active, executor);
      final BindableService adminService = new AdminServiceImpl(state, active);
      final BindableService crossServerService = new DistLedgerCrossServerServiceImpl(state, active);

      // Launch server
      final Server server =
          ServerBuilder.forPort(port).addService(userService).addService(adminService).addService(crossServerService).build();
      server.start();
      System.out.println("Server started, listening on " + port);

      // Register this server on the naming service
      final String target = InetAddress.getLocalHost().getHostAddress().toString() + ":" + port;
      try {
        final String address = InetAddress.getLocalHost().getHostAddress().toString();
        Logger.debug("Registering server at " + address + ":" + port);
        namingService.register(SERVICE_NAME, qualifier, address + ":" + port);

        // Add a shutdown hook to unregister the server
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                      try {
                        namingService.delete(SERVICE_NAME, qualifier);
                      } catch (RuntimeException e) {
                        Logger.error("Failed to unregister server: " + e.getMessage());
                      }
                    }));

        // Wait for user input to shutdown server
        System.out.println("Press enter to shutdown");
        System.in.read();
      } catch (RuntimeException e) {
        Logger.error("Failed to register server: " + e.getMessage());
      }

      // Wait until server is terminated
      server.shutdown();
      server.awaitTermination();
    }

    Logger.debug("Server terminated");
  }
}
