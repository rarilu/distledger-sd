package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import pt.tecnico.distledger.server.domain.ServerState;

public class ServerMain {
  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println(ServerMain.class.getSimpleName());

    // Receive and print arguments.
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    // Check arguments.
    if (args.length != 2) {
      System.err.println("Argument(s) missing!");
      System.err.println("Usage: mvn exec:java -Dexec.args=<port> <qual>");
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final String qualifier = args[1];

    // Init server state.
    final ServerState state = new ServerState();

    // Init services.
    final BindableService userService = new UserServiceImpl(state);

    // Launch server.
    final Server server = ServerBuilder.forPort(port).addService(userService).build();
    server.start();
    System.out.println("Server started, listening on " + port);

    // Wait until server is terminated.
    server.awaitTermination();
  }
}
