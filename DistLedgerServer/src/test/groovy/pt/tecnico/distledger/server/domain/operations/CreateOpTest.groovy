package pt.tecnico.distledger.server.domain.operations

import spock.lang.Specification

import pt.tecnico.distledger.common.domain.VectorClock
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException
import pt.tecnico.distledger.server.visitors.OperationExecutor

class CreateOpTest extends Specification {
    def state
    def executor
    def ts = new VectorClock()

    def setup() {
        state = new ServerState(0)
        executor = new OperationExecutor(state)
    }

    def "create a new account"() {
        when: "a new account is created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        then: "there are exactly two accounts"
        state.getAccounts().size() == 2

        and: "the new account has the correct balance"
        state.getAccounts().get("Alice").getBalance() == 0
    }

    def "create a duplicate account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        when: "an account with the same name is created"
        executor.execute(new CreateOp("Alice", ts, ts, 0))

        then: "an exception is thrown"
        thrown(AccountAlreadyExistsException)

        and: "the number of accounts is still 2"
        state.getAccounts().size() == 2
    }
}
