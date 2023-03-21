package pt.tecnico.distledger.server.domain.operations

import spock.lang.Specification
import java.util.concurrent.ConcurrentMap
import pt.tecnico.distledger.server.domain.Account
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.DeleteOp
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException
import pt.tecnico.distledger.server.domain.exceptions.ProtectedAccountException
import pt.tecnico.distledger.server.domain.exceptions.NonEmptyAccountException
import pt.tecnico.distledger.server.visitors.StandardOperationExecutor

class DeleteOpTest extends Specification {
    def state
    def executor

    def setup() {
        state = new ServerState()
        executor = new StandardOperationExecutor(state)
    }

    def "delete an account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))
        state.getAccounts().size() == 2

        when: "the account is deleted"
        executor.execute(new DeleteOp("Alice"))

        then: "there is only one account"
        state.getAccounts().size() == 1
    }

    def "delete the broker account"() {
        when: "the broker account is deleted"
        executor.execute(new DeleteOp("broker"))

        then: "an exception is thrown"
        thrown(ProtectedAccountException)

        and: "the broker account still exists"
        state.getAccounts().containsKey("broker")
    }

    def "delete a non-existing account"() {
        when: "an unknown account is deleted"
        executor.execute(new DeleteOp("void"))

        then: "an exception is thrown"
        thrown(UnknownAccountException)
    }

    def "delete a non-empty account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))

        and: "with a balance in the account"
        state.getAccounts().get("Alice").setBalance(10)

        when: "the account is deleted"
        executor.execute(new DeleteOp("Alice"))

        then: "an exception is thrown"
        thrown(NonEmptyAccountException)

        and: "the account still exists"
        state.getAccounts().containsKey("Alice")
    }

    def "delete before entering the sync block"() {
        given: "a mock state"
        def state = Mock(ServerState)
        def accounts = Mock(ConcurrentMap)
        state.getAccounts() >> accounts

        and: "an executor with the mock state"
        def executor = new StandardOperationExecutor(state)
        
        and: "first call to get returns an account"
        1 * accounts.get("Alice") >> new Account()

        and: "first call to containsKey returns false"
        1 * accounts.containsKey("Alice") >> false

        when: "the account is deleted"
        executor.execute(new DeleteOp("Alice"))

        then: "an exception is thrown"
        thrown(UnknownAccountException)
    }
}
