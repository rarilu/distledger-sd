package pt.tecnico.distledger.server.domain.operations

import spock.lang.Specification
import pt.tecnico.distledger.common.domain.VectorClock
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException
import pt.tecnico.distledger.server.domain.exceptions.NopTransferException
import pt.tecnico.distledger.server.domain.exceptions.NonPositiveTransferException
import pt.tecnico.distledger.server.visitors.OperationExecutor

class TransferOpTest extends Specification {
    def state
    def executor
    def ts = new VectorClock()

    def setup() {
        state = new ServerState(0)
        executor = new OperationExecutor(state)
    }

    def "transfer from broker to a new user"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        when: "a transfer is made from the broker to the new user"
        executor.execute(new TransferOp("broker", "Alice", 100, ts, ts, 0))

        then: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 900
        state.getAccounts().get("Alice").getBalance() == 100
    }

    def "transfer all of the balance to a new user"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        when: "a transfer is made from the broker to the new user"
        executor.execute(new TransferOp("broker", "Alice", 1000, ts, ts, 0))

        then: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 0
        state.getAccounts().get("Alice").getBalance() == 1000
    }

    def "transfer from non-existing account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        when: "a transfer is made from a non-existing account"
        executor.execute(new TransferOp("void", "Alice", 100, ts, ts, 0))

        then: "an exception is thrown"
        thrown(UnknownAccountException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }

    def "transfer to non-existing account"() {
        when: "a transfer is made to a non-existing account"
        executor.execute(new TransferOp("broker", "void", 100, ts, ts, 0))

        then: "an exception is thrown"
        thrown(UnknownAccountException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
    }

    def "transfer without enough balance"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        when: "a too large transfer is made from the broker to the new user"
        executor.execute(new TransferOp("broker", "Alice", 1001, ts, ts, 0))

        then: "an exception is thrown"
        thrown(NotEnoughBalanceException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }

    def "transfer from account to itself"() {
        when: "a transfer is made from the broker to itself"
        executor.execute(new TransferOp("broker", "broker", 100, ts, ts, 0))

        then: "an exception is thrown"
        thrown(NopTransferException)

        and: "the broker has the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
    }

    def "transfer non-positive amount"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        when: "a transfer is made with a non-positive amount"
        executor.execute(new TransferOp("broker", "Alice", -100, ts, ts, 0))

        then: "an exception is thrown"
        thrown(NonPositiveTransferException)

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }
}
