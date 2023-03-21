package pt.tecnico.distledger.server

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import pt.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest
import pt.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.visitors.DummyOperationExecutor
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class ReadOnlyUserServiceImplTest extends Specification {
    def service
    def observer

    def setup() {
        def state = new ServerState()
        def active = new AtomicBoolean(true)
        def executor = new DummyOperationExecutor()
        service = new UserServiceImpl(state, active, executor)
        observer = Mock(StreamObserver)
    }

    def "create account on read-only server fails"() {
        when: "create account is called"
        service.createAccount(CreateAccountRequest.newBuilder()
                .setUserId("Alice")
                .build(),
                observer)

        then: "invocation fails with InvalidWriteOperationException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage()
                    == "UNIMPLEMENTED: Invalid write operation on read-only server"
        })
    }

    def "delete account on read-only server fails"() {
        when: "delete account is called"
        service.deleteAccount(DeleteAccountRequest.newBuilder()
                .setUserId("Alice")
                .build(),
                observer)

        then: "invocation fails with InvalidWriteOperationException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage()
                    == "UNIMPLEMENTED: Invalid write operation on read-only server"
        })
    }

    def "transfer on read-only server fails"() {
        when: "transfer to is called"
        service.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom("broker")
                .setAccountTo("void")
                .setAmount(100)
                .build(),
                observer)

        then: "invocation fails with InvalidWriteOperationException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage()
                    == "UNIMPLEMENTED: Invalid write operation on read-only server"
        })
    }
}
