package pt.tecnico.distledger.server

import io.grpc.stub.StreamObserver
import java.util.concurrent.atomic.AtomicBoolean;
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.exceptions.UnknownAccountException
import pt.tecnico.distledger.server.domain.exceptions.ServerUnavailableException
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse
import spock.lang.Specification

class UserServiceImplTest extends Specification {
    def state
    def active
    def service

    def setup() {
        state = new ServerState()
        active = new AtomicBoolean(true)
        service = new UserServiceImpl(state, active)
    }

    def "deactivate server"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        when: "server is deactivated"
        active.set(false)

        and: "method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method is unavailable"
        1 * observer.onError({
            it instanceof ServerUnavailableException && it.getMessage() == "UNAVAILABLE: Server is unavailable"
        })

        where:
        method << UserServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class }
    }

    def "get balance for existing account"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        and: "an account already created"
        state.registerOperation(new CreateOp("Alice"))

        and: "with a given balance"
        if (balance > 0) {
            state.registerOperation(new TransferOp("broker", "Alice", balance))
        }

        when: "get balance for account"
        service.balance(BalanceRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "balance is correct"
        1 * observer.onNext(BalanceResponse.newBuilder().setValue(balance).build())

        where:
        balance << [0, 100]
    }

    def "get balance for non-existing account"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        when: "get balance for account"
        service.balance(BalanceRequest.newBuilder().setUserId("void").build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof UnknownAccountException && it.getMessage() == "NOT_FOUND: Account void does not exist"
        })
    }

    def "create account"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        when: "account is created"
        service.createAccount(CreateAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "account is created"
        1 * observer.onNext(CreateAccountResponse.getDefaultInstance())
    }

    def "delete account"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        and: "an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "account is deleted"
        service.deleteAccount(DeleteAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "account is deleted"
        1 * observer.onNext(DeleteAccountResponse.getDefaultInstance())
    }

    def "transfer between accounts"() {
        given: "a mock observer"
        def observer = Mock(StreamObserver)

        and: "an account already created"
        state.registerOperation(new CreateOp("Alice"))

        when: "a transfer is made"
        service.transferTo(TransferToRequest.newBuilder().setAccountFrom("broker").setAccountTo("Alice").setAmount(100).build(), observer)

        then: "the transfer is made"
        1 * observer.onNext(TransferToResponse.getDefaultInstance())
    }
}
