package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.client.BaseCommandParser;
import pt.tecnico.distledger.userclient.grpc.UserService;

/** Parses the input from the user and executes the corresponding commands. */
public class CommandParser extends BaseCommandParser {
  private static final String CREATE_ACCOUNT = "createAccount";
  private static final String TRANSFER_TO = "transferTo";
  private static final String BALANCE = "balance";
  private static final String TIMESTAMP = "timestamp";

  private final UserService userService;

  public CommandParser(UserService userService) {
    this.userService = userService;
  }

  @Override
  protected void dispatchCommand(String cmd, String line) {
    switch (cmd) {
      case CREATE_ACCOUNT -> this.createAccount(line);
      case TRANSFER_TO -> this.transferTo(line);
      case BALANCE -> this.balance(line);
      case TIMESTAMP -> this.handleServiceCallResponse(this.userService::timestamp);
      default -> {
        Logger.debug("Unknown command: " + cmd);
        this.printUsage();
      }
    }
  }

  private void createAccount(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 3) {
      this.printUsage();
      return;
    }

    String server = split[1];
    String username = split[2];

    this.handleServiceCallResponse(() -> this.userService.createAccount(server, username));
  }

  private void balance(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 3) {
      this.printUsage();
      return;
    }

    String server = split[1];
    String username = split[2];

    this.handleServiceCallResponse(() -> this.userService.balance(server, username));
  }

  private void transferTo(String line) throws NumberFormatException {
    String[] split = line.split(SPACE);
    if (split.length != 5) {
      this.printUsage();
      return;
    }

    try {
      String server = split[1];
      String from = split[2];
      String dest = split[3];
      int amount = Integer.parseInt(split[4]);

      this.handleServiceCallResponse(() -> this.userService.transferTo(server, from, dest, amount));
    } catch (NumberFormatException e) {
      Logger.debug(e.getMessage());
      System.out.println("Error: Invalid number provided");
    }
  }

  @Override
  protected void printUsage() {
    System.out.println(
        "Usage:\n"
            + "- createAccount <server> <username>\n"
            + "- balance <server> <username>\n"
            + "- transferTo <server> <username_from> <username_to> <amount>\n"
            + "- timestamp\n"
            + "- exit\n");
  }
}
