import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException
import pt.tecnico.distledger.server.exceptions.UnknownAccountException
import pt.tecnico.distledger.server.exceptions.NotEnoughBalanceException
import pt.tecnico.distledger.server.exceptions.NopTransferException
import pt.tecnico.distledger.server.exceptions.NonPositiveTransferException

class ServerStateTest extends Specification {
    def "state has broker account with 1000"() {
        when: "a new server state is created"
        def state = new ServerState()

        then: "exactly one account exists"
        state.getAccounts().size() == 1        

        and: "the broker account has the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
    }

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
        def e = thrown(AccountAlreadyExistsException)
        e.getGrpcStatus().getCode() == io.grpc.Status.ALREADY_EXISTS.getCode()

        and: "the number of accounts is still 2"
        state.getAccounts().size() == 2
    }

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

    def "transfer from non-existing account"() {
        given: "a new server state"
        def state = new ServerState()

        and: "with an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a transfer is made from a non-existing account"
        state.registerOperation(new TransferOp("void", "Alice", 100))

        then: "an exception is thrown"
        def e = thrown(UnknownAccountException)
        e.getGrpcStatus().getCode() == io.grpc.Status.NOT_FOUND.getCode()

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
        def e = thrown(UnknownAccountException)
        e.getGrpcStatus().getCode() == io.grpc.Status.NOT_FOUND.getCode()

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
        def e = thrown(NotEnoughBalanceException)
        e.getGrpcStatus().getCode() == io.grpc.Status.FAILED_PRECONDITION.getCode()

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
        def e = thrown(NopTransferException)
        e.getGrpcStatus().getCode() == io.grpc.Status.INVALID_ARGUMENT.getCode()

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
        def e = thrown(NonPositiveTransferException)
        e.getGrpcStatus().getCode() == io.grpc.Status.INVALID_ARGUMENT.getCode()

        and: "the accounts have the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
        state.getAccounts().get("Alice").getBalance() == 0
    }
}
