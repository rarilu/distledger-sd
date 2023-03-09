package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.tecnico.distledger.utils.Logger;

public class UserClientMain {
  public static void main(String[] args) {
    Logger.debug(UserClientMain.class.getSimpleName());

    // check arguments
    if (args.length != 2) {
      Logger.error("Argument(s) missing!");
      Logger.error("Usage: mvn exec:java -Dexec.args=<host> <port>");
      return;
    }

    final String host = args[0];
    final int port = Integer.parseInt(args[1]);

    try (final UserService userService = new UserService(host, port)) {
      CommandParser parser = new CommandParser(userService);
      parser.parseInput();
    }
  }
}
