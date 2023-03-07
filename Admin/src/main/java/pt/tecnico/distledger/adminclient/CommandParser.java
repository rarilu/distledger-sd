package pt.tecnico.distledger.adminclient;

import java.util.Scanner;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.utils.Logger;

public class CommandParser {
  private static final String SPACE = " ";
  private static final String ACTIVATE = "activate";
  private static final String DEACTIVATE = "deactivate";
  private static final String GET_LEDGER_STATE = "getLedgerState";
  private static final String GOSSIP = "gossip";
  private static final String HELP = "help";
  private static final String EXIT = "exit";

  private final AdminService adminService;

  public CommandParser(AdminService adminService) {
    this.adminService = adminService;
  }

  public void parseInput() {
    boolean exit = false;

    try (final Scanner scanner = new Scanner(System.in)) {
      while (!exit) {
        System.out.print("> ");
        String line = scanner.nextLine().trim();
        String cmd = line.split(SPACE)[0];

        switch (cmd) {
          case ACTIVATE -> this.activate(line);
          case DEACTIVATE -> this.deactivate(line);
          case GET_LEDGER_STATE -> this.dump(line);
          case GOSSIP -> this.gossip(line);
          case HELP -> this.printUsage();
          case EXIT -> exit = true;
          default -> {
            Logger.debug("Unknown command: " + cmd);
            this.printUsage();
          }
        }
      }
    }
  }

  private void activate(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String server = split[1];

    this.adminService.activate(server);
  }

  private void deactivate(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String server = split[1];

    this.adminService.deactivate(server);
  }

  private void dump(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String server = split[1];

    System.out.println("TODO: implement getLedgerState command");
  }

  @SuppressWarnings("unused")
  private void gossip(String line) {
    /* TODO Phase-3 */
    System.out.println("TODO: implement gossip command (only for Phase-3)");
  }

  private void printUsage() {
    System.out.println(
        "Usage:\n"
            + "- activate <server>\n"
            + "- deactivate <server>\n"
            + "- getLedgerState <server>\n"
            + "- gossip <server>\n"
            + "- exit\n");
  }
}
