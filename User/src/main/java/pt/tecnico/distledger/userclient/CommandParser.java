package pt.tecnico.distledger.userclient;

import java.util.NoSuchElementException;
import java.util.Scanner;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.tecnico.distledger.utils.Logger;

public class CommandParser {
  private static final String SPACE = " ";
  private static final String CREATE_ACCOUNT = "createAccount";
  private static final String DELETE_ACCOUNT = "deleteAccount";
  private static final String TRANSFER_TO = "transferTo";
  private static final String BALANCE = "balance";
  private static final String HELP = "help";
  private static final String EXIT = "exit";

  private final UserService userService;

  public CommandParser(UserService userService) {
    this.userService = userService;
  }

  public void parseInput() {
    boolean exit = false;

    try (final Scanner scanner = new Scanner(System.in)) {
      while (!exit) {
        try {
          System.out.print("> ");
          String line = scanner.nextLine().trim();
          String cmd = line.split(SPACE)[0];

          switch (cmd) {
            case CREATE_ACCOUNT -> this.createAccount(line);
            case DELETE_ACCOUNT -> this.deleteAccount(line);
            case TRANSFER_TO -> this.transferTo(line);
            case BALANCE -> this.balance(line);
            case HELP -> this.printUsage();
            case EXIT -> exit = true;
            default -> {
              Logger.debug("Unknown command: " + cmd);
              this.printUsage();
            }
          }
        } catch (NumberFormatException e) {
          Logger.debug(e.getMessage());
          System.out.println("Error: Invalid number provided");
        } catch (NoSuchElementException e) {
          exit = true;
        }
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

    this.userService.createAccount(server, username);
  }

  private void deleteAccount(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 3) {
      this.printUsage();
      return;
    }

    String server = split[1];
    String username = split[2];

    this.userService.deleteAccount(server, username);
  }

  private void balance(String line) {
    String[] split = line.split(SPACE);
    if (split.length != 3) {
      this.printUsage();
      return;
    }

    String server = split[1];
    String username = split[2];

    this.userService.balance(server, username);
  }

  private void transferTo(String line) throws NumberFormatException {
    String[] split = line.split(SPACE);
    if (split.length != 5) {
      this.printUsage();
      return;
    }

    String server = split[1];
    String from = split[2];
    String dest = split[3];
    int amount = Integer.parseInt(split[4]);

    userService.transferTo(server, from, dest, amount);
  }

  private void printUsage() {
    System.out.println(
        "Usage:\n"
            + "- createAccount <server> <username>\n"
            + "- deleteAccount <server> <username>\n"
            + "- balance <server> <username>\n"
            + "- transferTo <server> <username_from> <username_to> <amount>\n"
            + "- exit\n");
  }
}
