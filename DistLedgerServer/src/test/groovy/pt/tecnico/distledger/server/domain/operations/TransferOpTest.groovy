package pt.tecnico.distledger.server.domain.operations

import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException
import pt.tecnico.distledger.server.domain.exceptions.NopTransferException
import pt.tecnico.distledger.server.domain.exceptions.NonPositiveTransferException

class TransferOpTest extends Specification {
    def "transfer from broker to a new user"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a transfer is made from the broker to the new user"
        state.registerOperation(new TransferOp("broker", "Alice", 100))

        then: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 900
        state.getAccounts().get("Alice").getBalance() == 100
    }

    def "transfer all of the balance to a new user"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a transfer is made from the broker to the new user"
        state.registerOperation(new TransferOp("broker", "Alice", 1000))

        then: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 0
        state.getAccounts().get("Alice").getBalance() == 1000
    }

    def "transfer from non-existing account"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a transfer is made from a non-existing account"
        state.registerOperation(new TransferOp("void", "Alice", 100))

        then: "an exception is thrown"
        thrown(UnknownAccountException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }

    def "transfer to non-existing account"() {
        given: "a new server state"
        def state = new ServerState()

        when: "a transfer is made to a non-existing account"
        state.registerOperation(new TransferOp("broker", "void", 100))

        then: "an exception is thrown"
        thrown(UnknownAccountException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
    }

    def "transfer without enough balance"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a too large transfer is made from the broker to the new user"
        state.registerOperation(new TransferOp("broker", "Alice", 1001))

        then: "an exception is thrown"
        thrown(NotEnoughBalanceException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }

    def "transfer from account to itself"() {
        given: "a new server state"
        def state = new ServerState()

        when: "a transfer is made from the broker to itself"
        state.registerOperation(new TransferOp("broker", "broker", 100))

        then: "an exception is thrown"
        thrown(NopTransferException)

        and: "the broker has the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
    }

    def "transfer non-positive amount"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a transfer is made with a non-positive amount"
        state.registerOperation(new TransferOp("broker", "Alice", -100))

        then: "an exception is thrown"
        thrown(NonPositiveTransferException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }
}
