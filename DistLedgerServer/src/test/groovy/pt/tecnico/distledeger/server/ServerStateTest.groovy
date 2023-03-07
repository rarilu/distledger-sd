import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException

class ServerStateTest extends Specification {
    def "state has broker account with 1000"() {
        when:
        def state = new ServerState()

        then:
        state.getAccounts().size() == 1
        state.getAccounts().get("broker") == 1000
    }

    def "create a new account"() {
        given:
        def state = new ServerState()

        when:
        state.registerOperation(new CreateOp("alice"))

        then:
        state.getAccounts().size() == 2
        state.getAccounts().get("alice") == 0
    }

    def "create a duplicate account"() {
        given:
        def state = new ServerState()
        state.registerOperation(new CreateOp("alice"))

        when:
        state.registerOperation(new CreateOp("alice"))

        then:
        def e = thrown(AccountAlreadyExistsException)
        e.getGrpcStatus().getCode() == io.grpc.Status.ALREADY_EXISTS.getCode()
        state.getAccounts().size() == 2
    }
}
