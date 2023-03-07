package pt.tecnico.distledger.server.domain;

public class Account {
  private int balance;

  public Account(int balance) {
    this.balance = balance;
  }

  public Account() {
    this(0);
  }

  public int getBalance() {
    return this.balance;
  }
}
