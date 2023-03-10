package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.utils.Logger;

/** Main class for the Admin client. */
public class AdminClientMain {
  /** Main method. */
  public static void main(String[] args) {
    Logger.debug(AdminClientMain.class.getSimpleName());

    // check arguments
    if (args.length != 2) {
      Logger.error("Argument(s) missing!");
      Logger.error("Usage: mvn exec:java -Dexec.args=\"<host> <port>\"");
      return;
    }

    final String host = args[0];
    final int port = Integer.parseInt(args[1]);

    try (final AdminService adminService = new AdminService(host, port)) {
      CommandParser parser = new CommandParser(adminService);
      parser.parseInput();
    }
  }
}
