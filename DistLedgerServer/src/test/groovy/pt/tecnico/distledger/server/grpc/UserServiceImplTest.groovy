package pt.tecnico.distledger.server.grpc

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

import java.util.concurrent.atomic.AtomicBoolean

import pt.tecnico.distledger.server.DirectLedgerManager
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.exceptions.FailedPropagationException;
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.visitors.OperationExecutor
import pt.tecnico.distledger.server.visitors.StandardOperationExecutor
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse

import spock.lang.Specification

class UserServiceImplTest extends Specification {
    def state
    def executor
    def active
    def service
    def observer

    def setup() {
        state = new ServerState()
        def ledgerManager = new DirectLedgerManager(state)
        executor = new StandardOperationExecutor(state, ledgerManager)
        active = new AtomicBoolean(true)
        service = new UserServiceImpl(state, active, executor)
        observer = Mock(StreamObserver)
    }

    def "create account"() {
        when: "a new account is created"
        service.createAccount(CreateAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(CreateAccountResponse.getDefaultInstance())
    }

    def "create duplicate account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))

        when: "the account is created again"
        service.createAccount(CreateAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "ALREADY_EXISTS: Account for user Alice already exists"
        })
    }

    def "delete an existing account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))

        when: "the account is deleted"
        service.deleteAccount(DeleteAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(DeleteAccountResponse.getDefaultInstance())
    }

    def "delete a non-existing account"() {
        when: "a non-existing is deleted"
        service.deleteAccount(DeleteAccountRequest.newBuilder().setUserId("void").build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "NOT_FOUND: Account void does not exist"
        })
    }

    def "delete a protected account"() {
        when: "a protected account is deleted"
        service.deleteAccount(DeleteAccountRequest.newBuilder().setUserId("broker").build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "INVALID_ARGUMENT: Account for user broker is protected"
        })
    }

    def "delete a non-empty account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))

        and: "with a given balance"
        executor.execute(new TransferOp("broker", "Alice", 100))

        when: "the account is deleted"
        service.deleteAccount(DeleteAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "FAILED_PRECONDITION: Account for user Alice has 100 left, needs to be empty"
        })
    }

    def "transfer between accounts"() {
        given: "an accounts already created"
        executor.execute(new CreateOp("Alice"))

        when: "transfer between accounts"
        service.transferTo(TransferToRequest.newBuilder().setAccountFrom("broker").setAccountTo("Alice").setAmount(100).build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(TransferToResponse.getDefaultInstance())
    }

    def "transfer between accounts with insufficient funds"() {
        given: "an accounts already created"
        executor.execute(new CreateOp("Alice"))

        when: "transfer between accounts"
        service.transferTo(TransferToRequest.newBuilder().setAccountFrom("Alice").setAccountTo("broker").setAmount(100).build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "FAILED_PRECONDITION: Account Alice does not have enough balance to transfer 100"
        })
    }

    def "transfer to non-existing account"() {
        when: "transfer to non-existing account"
        service.transferTo(TransferToRequest.newBuilder().setAccountFrom("broker").setAccountTo("void").setAmount(100).build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "NOT_FOUND: Account void does not exist"
        })
    }

    def "transfer non-positive amount"() {
        given: "an accounts already created"
        executor.execute(new CreateOp("Alice"))

        when: "transfer non-positive amount"
        service.transferTo(TransferToRequest.newBuilder().setAccountFrom("broker").setAccountTo("Alice").setAmount(amount).build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "INVALID_ARGUMENT: Transfers with non-positive amount are not allowed"
        })

        where:
        amount << [0, -100]
    }

    def "transfer to the same account"() {
        when: "transfer to the same account"
        service.transferTo(TransferToRequest.newBuilder().setAccountFrom("broker").setAccountTo("broker").setAmount(100).build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "INVALID_ARGUMENT: Transfers from an account to itself are not allowed"
        })
    }

    def "get balance for existing account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))

        and: "with a given balance"
        if (balance > 0) {
            executor.execute(new TransferOp("broker", "Alice", balance))
        }

        when: "get balance for account"
        service.balance(BalanceRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(BalanceResponse.newBuilder().setValue(balance).build())

        where:
        balance << [0, 100]
    }

    def "get balance for non-existing account"() {
        when: "get balance for account"
        service.balance(BalanceRequest.newBuilder().setUserId("void").build(), observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "NOT_FOUND: Account void does not exist"
        })
    }

    def "deactivate server"() {
        when: "server is deactivated"
        active.set(false)

        and: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with ServerUnavailableException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNAVAILABLE: Server is unavailable"
        })

        where: "method is any void function of UserServiceImpl"
        method << UserServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class }
    }

    def "catch failed propagations"() {
        given: "a mocked executor that throws an FailedPropagationException when used"
        def executor = Mock(OperationExecutor)
        executor.execute(_) >> { throw new FailedPropagationException("Failed propagation") }

        and: "a service with the mocked state and executor"
        def service = new UserServiceImpl(state, active, executor)

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "ABORTED: Failed propagation"
        })

        where: "method is any void function of UserServiceImpl not in thhe ignore list"
        method << UserServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class && ![ "balance" ].contains(it.getName()) }
    }

    def "catch runtime exceptions"() {
        given: "a state that throws an exception when used"
        def state = Mock(ServerState)
        state.getAccountBalance(_) >> { throw new RuntimeException("Unknown error") }

        and: "a mocked executor that throws an exception when used"
        def executor = Mock(OperationExecutor)
        executor.execute(_) >> { throw new RuntimeException("Unknown error") }

        and: "a service with the mocked state and executor"
        def service = new UserServiceImpl(state, active, executor)

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any void function of UserServiceImpl"
        method << UserServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class }
    }
}
