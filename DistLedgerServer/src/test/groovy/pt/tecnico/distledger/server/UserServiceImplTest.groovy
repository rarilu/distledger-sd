package pt.tecnico.distledger.server

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.domain.operation.CreateOp
import pt.tecnico.distledger.server.domain.operation.TransferOp
import pt.tecnico.distledger.server.visitors.OperationExecutor
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse
import spock.lang.Specification

class UserServiceImplTest extends Specification {
    def executor
    def active
    def service
    def observer

    def setup() {
        def state = new ServerState()
        executor = new OperationExecutor(state)
        active = new AtomicBoolean(true)
        service = new UserServiceImpl(state, active)
        observer = Mock(StreamObserver)
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

    def "get balance for existing account"() {
        given: "an account already created"
        executor.execute(new CreateOp("Alice"))

        and: "with a given balance"
        if (balance > 0) {
            executor.execute(new TransferOp("broker", "Alice", balance))
        }

        when: "get balance for account"
        service.balance(BalanceRequest.newBuilder().setUserId("Alice").build(), observer)

        then: "balance is correct"
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

    def "catch runtime exceptions"() {
        given: "a state that throws an exception when used"
        def state = Mock(ServerState)
        state.getAccountBalance(_) >> { throw new RuntimeException("Unknown error") }

        and: "a service with the mocked state"
        def service = new UserServiceImpl(state, active)

        and: "a mocked executor that throws an exception when used"
        Field field = service.class.getDeclaredField("executor")
        field.setAccessible(true)
        field.set(service, Mock(OperationExecutor))
        service.executor.execute(_) >> { throw new RuntimeException("Unknown error") }

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any void function of UserServiceImpl"
        method << UserServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class }
    }

    def setFinalTo(obj, name, value) {
        
    }
}
