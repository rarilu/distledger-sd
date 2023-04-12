package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.client.BaseCommandParser;

/** Parses the input from the user and executes the corresponding commands. */
public class CommandParser extends BaseCommandParser {
  private static final String ACTIVATE = "activate";
  private static final String DEACTIVATE = "deactivate";
  private static final String GET_LEDGER_STATE = "getLedgerState";
  private static final String GOSSIP = "gossip";

  private final AdminService adminService;

  public CommandParser(AdminService adminService) {
    this.adminService = adminService;
  }

  @Override
  protected void dispatchCommand(String cmd, String line) {
    switch (cmd) {
      case ACTIVATE -> this.activate(line);
      case DEACTIVATE -> this.deactivate(line);
      case GET_LEDGER_STATE -> this.getLedgerState(line);
      case GOSSIP -> this.gossip(line);
      default -> {
        Logger.debug("Unknown command: " + cmd);
        this.printUsage();
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

    this.handleServiceCallResponse(() -> this.adminService.activate(server));
  }

  private void deactivate(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String server = split[1];

    this.handleServiceCallResponse(() -> this.adminService.deactivate(server));
  }

  private void getLedgerState(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String server = split[1];

    this.handleServiceCallResponse(() -> this.adminService.getLedgerState(server));
  }

  private void gossip(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 2) {
      this.printUsage();
      return;
    }

    String server = split[1];

    this.handleServiceCallResponse(() -> this.adminService.gossip(server));
  }

  @Override
  protected void printUsage() {
    System.out.println(
        "Usage:\n"
            + "- activate <server>\n"
            + "- deactivate <server>\n"
            + "- getLedgerState <server>\n"
            + "- gossip <server>\n"
            + "- exit\n");
  }
}
