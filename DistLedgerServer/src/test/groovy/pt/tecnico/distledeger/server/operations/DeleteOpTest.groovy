import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.DeleteOp
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException
import pt.tecnico.distledger.server.domain.exceptions.ProtectedAccountException
import pt.tecnico.distledger.server.domain.exceptions.NonEmptyAccountException

class DeleteOpTest extends Specification {
    def "delete an account"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "the account is deleted"
        state.registerOperation(new DeleteOp("Alice"))

        then: "there is only one account"
        state.getAccounts().size() == 1
    }

    def "delete the broker account"() {
        given: "a new server state"
        def state = new ServerState()

        when: "the broker account is deleted"
        state.registerOperation(new DeleteOp("broker"))

        then: "an exception is thrown"
        def e = thrown(ProtectedAccountException)
        e.getGrpcStatus().getCode() == io.grpc.Status.FAILED_PRECONDITION.getCode()
    }

    def "delete a non-existing account"() {
        given: "a new server state"
        def state = new ServerState()

        when: "an unknown account is deleted"
        state.registerOperation(new DeleteOp("Alice"))

        then: "an exception is thrown"
        def e = thrown(UnknownAccountException)
        e.getGrpcStatus().getCode() == io.grpc.Status.NOT_FOUND.getCode()
    }

    def "delete a non-empty account"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        and: "with a balance in the account"
        state.getAccounts().get("Alice").setBalance(10)

        when: "the account is deleted"
        state.registerOperation(new DeleteOp("Alice"))

        then: "an exception is thrown"
        def e = thrown(NonEmptyAccountException)
        e.getGrpcStatus().getCode() == io.grpc.Status.FAILED_PRECONDITION.getCode()
    }
}
