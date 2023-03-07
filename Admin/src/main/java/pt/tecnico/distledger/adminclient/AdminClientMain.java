package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.utils.Logger;

public class AdminClientMain {
  public static void main(String[] args) {
    Logger.debug(AdminClientMain.class.getSimpleName());

    // check arguments
    if (args.length != 2) {
      System.err.println("Argument(s) missing!");
      System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port>");
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
