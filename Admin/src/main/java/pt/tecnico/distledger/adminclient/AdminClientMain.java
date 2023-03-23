package pt.tecnico.distledger.adminclient;

import java.util.Arrays;
import java.util.Optional;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.NamingService;

/** Main class for the Admin client. */
public class AdminClientMain {
  /** Main method. */
  public static void main(String[] args) {
    Logger.debug(AdminClientMain.class.getSimpleName());

    // accepts naming server target as an optional argument
    // if not provided, uses the well-known target
    Optional<String> namingServerTarget = Arrays.stream(args).findFirst();

    try (final NamingService namingService =
        namingServerTarget.map(NamingService::new).orElseGet(NamingService::new)) {
      try (final AdminService adminService = new AdminService(namingService)) {
        CommandParser parser = new CommandParser(adminService);
        parser.parseInput();
      }
    }
  }
}
