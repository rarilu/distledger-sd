package pt.tecnico.distledger.userclient;

import java.util.Arrays;
import java.util.Optional;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.grpc.NamingService;
import pt.tecnico.distledger.userclient.grpc.UserService;

/** Main class for the User client. */
public class UserClientMain {
  /** Main method. */
  public static void main(String[] args) {
    Logger.debug(UserClientMain.class.getSimpleName());

    // accepts naming server target as an optional argument
    // if not provided, uses the well-known target
    Optional<String> namingServerTarget = Arrays.stream(args).findFirst();

    try (final NamingService namingService =
        namingServerTarget.map(NamingService::new).orElseGet(NamingService::new)) {
      try (final UserService userService = new UserService(namingService)) {
        CommandParser parser = new CommandParser(userService);
        parser.parseInput();
      }
    }
  }
}
