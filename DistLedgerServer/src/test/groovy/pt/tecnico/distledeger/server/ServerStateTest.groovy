import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException

class ServerStateTest extends Specification {
    def "state has broker account with 1000"() {
        when: "a new server state is created"
        def state = new ServerState()

        then: "exactly one account exists"
        state.getAccounts().size() == 1        

        and: "the broker account has the correct balance"
        state.getAccounts().get("broker") == 1000
    }

    def "create a new account"() {
        given: "a new server state"
        def state = new ServerState()

        when: "a new account is created"
        state.registerOperation(new CreateOp("alice"))

        then: "there are exactly two accounts"
        state.getAccounts().size() == 2

        and: "the new account has the correct balance"
        state.getAccounts().get("alice") == 0
    }

    def "create a duplicate account"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("alice"))

        when: "an account with the same name is created"
        state.registerOperation(new CreateOp("alice"))

        then: "an exception is thrown"
        def e = thrown(AccountAlreadyExistsException)
        e.getGrpcStatus().getCode() == io.grpc.Status.ALREADY_EXISTS.getCode()

        and: "the number of accounts is still 2"
        state.getAccounts().size() == 2
    }
}
