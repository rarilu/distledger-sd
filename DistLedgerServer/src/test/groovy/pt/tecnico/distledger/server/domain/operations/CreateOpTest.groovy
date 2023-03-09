import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException

class CreateOpTest extends Specification {
    def "create a new account"() {
        given: "a new server state"
        def state = new ServerState()

        when: "a new account is created"
        state.registerOperation(new CreateOp("Alice"))

        then: "there are exactly two accounts"
        state.getAccounts().size() == 2

        and: "the new account has the correct balance"
        state.getAccounts().get("Alice").getBalance() == 0
    }

    def "create a duplicate account"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "an account with the same name is created"
        state.registerOperation(new CreateOp("Alice"))

        then: "an exception is thrown"
        thrown(AccountAlreadyExistsException)

        and: "the number of accounts is still 2"
        state.getAccounts().size() == 2
    }
}
