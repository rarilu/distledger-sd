package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.tecnico.distledger.server.grpc.DistLedgerCrossServerServiceImpl;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;

/** Main class for the DistLedger server. */
public class ServerMain {
  private static final String SERVICE_NAME = "DistLedger";

  /** Main method. */
  public static void main(String[] args) throws IOException, InterruptedException {
    Logger.debug(ServerMain.class.getSimpleName());

    // Check arguments
    if (args.length < 2) {
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
      // Init the cross server service
      try (final CrossServerService crossServerService = new CrossServerService(namingService)) {
        // Register this server on the naming service
        final String target = InetAddress.getLocalHost().getHostAddress() + ":" + port;
        AtomicBoolean registered = new AtomicBoolean(false);

        // 'Thread' used to unregister the server from the naming service
        Thread autoUnregister =
            new Thread(
                () -> {
                  if (registered.getAndSet(false)) {
                    try {
                      namingService.delete(SERVICE_NAME, target);
                    } catch (RuntimeException e) {
                      Logger.error("Failed to unregister server: " + e.getMessage());
                    }

                    namingService.close();
                  }
                });

        try {
          int assignedId; // Assigned ID by the naming service

          try {
            Logger.debug("Registering server at " + target);
            assignedId = namingService.register(SERVICE_NAME, qualifier, target);
            registered.set(true);
            Runtime.getRuntime().addShutdownHook(autoUnregister);
          } catch (RuntimeException e) {
            Logger.error("Failed to register server: " + e.getMessage());
            return;
          }

          // Init server state - we need to do this after registering the server so that we can
          // pass the assigned ID to the server state
          final ServerState state = new ServerState(assignedId);

          // Init active flag
          final AtomicBoolean active = new AtomicBoolean(true);

          // Init service implementations
          final BindableService userServiceImpl = new UserServiceImpl(state, active);
          final BindableService adminServiceImpl = new AdminServiceImpl(state, active);
          final BindableService crossServerServiceImpl =
              new DistLedgerCrossServerServiceImpl(state, active);

          // Launch server
          final Server server =
              ServerBuilder.forPort(port)
                  .addService(userServiceImpl)
                  .addService(adminServiceImpl)
                  .addService(crossServerServiceImpl)
                  .build();
          server.start();
          System.out.println("Server started, listening on " + port);

          // Wait for user input to shut down server
          System.out.println("Press enter to shutdown");
          System.in.read();

          // Wait until server is terminated
          server.shutdown();
          server.awaitTermination();
        } finally {
          autoUnregister.run();
        }
      }
    }

    Logger.debug("Server terminated");
  }
}
