package pt.tecnico.distledger.server.grpc

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

import java.util.concurrent.atomic.AtomicBoolean

import pt.tecnico.distledger.common.domain.VectorClock
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.visitors.OperationExecutor

import pt.tecnico.distledger.contract.DistLedgerCommonDefinitions
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse

import spock.lang.Specification

class UserServiceImplTest extends Specification {
    def state
    def active
    def service
    def observer
    def executor

    def setup() {
        state = new ServerState(0)
        active = new AtomicBoolean(true)
        service = new UserServiceImpl(state, active)
        observer = Mock(StreamObserver)
        executor = new OperationExecutor(state)
    }

    def "create account"() {
        when: "a new account is created"
        service.createAccount(CreateAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(CreateAccountResponse.newBuilder().setValueTS(
            DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build()).build())

        and: "the ledger is updated correctly"
        state.ledger[0].isStable() && !state.ledger[0].hasFailed()
    }

    def "create duplicate account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", new VectorClock(), new VectorClock(), 0))

        when: "the account is created again"
        service.createAccount(CreateAccountRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(CreateAccountResponse.newBuilder().setValueTS(
            DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build()).build())

        and: "the ledger is updated correctly"
        state.ledger[0].isStable() && state.ledger[0].hasFailed()
    }

    def "transfer between accounts"() {
        given: "an accounts already created"
        executor.execute(new CreateOp("Alice", new VectorClock(), new VectorClock(), 0))

        when: "transfer between accounts"
        service.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom("broker")
                .setAccountTo("Alice")
                .setAmount(100)
                .build(),
                observer)

        then: "the correct response is received"
        1 * observer.onNext(TransferToResponse.newBuilder().setValueTS(
            DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build()).build())

        and: "the ledger is updated correctly"
        state.ledger[0].isStable() && !state.ledger[0].hasFailed()
    }

    def "transfer between accounts with insufficient funds"() {
        given: "an accounts already created"
        executor.execute(new CreateOp("Alice", new VectorClock(), new VectorClock(), 0))

        when: "transfer between accounts"
        service.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom("Alice")
                .setAccountTo("broker")
                .setAmount(100)
                .build(),
                observer)

        then: "the correct response is received"
        1 * observer.onNext(TransferToResponse.newBuilder().setValueTS(
            DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build()).build())
        
        and: "the transfer was not executed"
        state.getAccountBalance("Alice", new VectorClock()).value() == 0
        state.getAccountBalance("broker", new VectorClock()).value() == 1000

        and: "the ledger is updated correctly"
        state.ledger[0].isStable() && state.ledger[0].hasFailed()
    }

    def "transfer to non-existing account"() {
        when: "transfer to non-existing account"
        service.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom("broker")
                .setAccountTo("void")
                .setAmount(100)
                .build(),
                observer)

        then: "the correct response is received"
        1 * observer.onNext(TransferToResponse.newBuilder().setValueTS(
            DistLedgerCommonDefinitions.VectorClock.newBuilder().addValues(1).build()).build())
        
        and: "the transfer was not executed"
        state.getAccountBalance("broker", new VectorClock()).value() == 1000

        and: "the ledger is updated correctly"
        state.ledger[0].isStable() && state.ledger[0].hasFailed()
    }

    def "transfer non-positive amount"() {
        given: "an accounts already created"
        executor.execute(new CreateOp("Alice", new VectorClock(), new VectorClock(), 0))

        when: "transfer non-positive amount"
        service.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom("broker")
                .setAccountTo("Alice")
                .setAmount(amount)
                .build(),
                observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException
                    && it.getMessage() == "INVALID_ARGUMENT: Transfers with non-positive amount are not allowed"
        })
        
        and: "the transfer was not executed"
        state.getAccountBalance("broker", new VectorClock()).value() == 1000
        state.getAccountBalance("Alice", new VectorClock()).value() == 0

        and: "the transfer was not added to the ledger"
        state.ledger.size() == 0

        where:
        amount << [0, -100]
    }

    def "transfer to the same account"() {
        when: "transfer to the same account"
        service.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom("broker")
                .setAccountTo("broker")
                .setAmount(100)
                .build(),
                observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException
                    && it.getMessage() == "INVALID_ARGUMENT: Transfers from an account to itself are not allowed"
        })
        
        and: "the transfer was not executed"
        state.getAccountBalance("broker", new VectorClock()).value() == 1000

        and: "the transfer was not added to the ledger"
        state.ledger.size() == 0
    }

    def "get balance for existing account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice", new VectorClock(), new VectorClock(), 0))

        and: "with a given balance"
        if (balance > 0) {
            executor.execute(new TransferOp("broker", "Alice", balance, new VectorClock(), new VectorClock(), 0))
        }

        when: "get balance for account"
        service.balance(BalanceRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext(BalanceResponse.newBuilder().setValue(balance).setValueTS(
            DistLedgerCommonDefinitions.VectorClock.getDefaultInstance()).build())

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

    def "catch runtime exceptions"() {
        given: "a state that throws an exception when used"
        def state = Mock(ServerState, constructorArgs:[0])
        state.getAccountBalance(_, _) >> { throw new RuntimeException("Unknown error") }
        state.generateTimeStamp() >> { throw new RuntimeException("Unknown error") }

        and: "a service with the mocked state"
        def service = new UserServiceImpl(state, active)

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
