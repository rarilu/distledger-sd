package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.NamingService;

/** Main class for the Admin client. */
public class AdminClientMain {
  /** Main method. */
  public static void main(String[] args) {
    Logger.debug(AdminClientMain.class.getSimpleName());


    try (final NamingService namingService = new NamingService()) {
      try (final AdminService adminService = new AdminService(namingService)) {
        CommandParser parser = new CommandParser(adminService);
        parser.parseInput();
      }
    }
  }
}
