package pt.tecnico.distledger.server

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import pt.tecnico.distledger.server.domain.ServerState
import pt.tecnico.distledger.server.visitors.DummyOperationExecutor
import spock.lang.Specification
import spock.lang.Unroll

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

    @Unroll
    def "write operation '#methodName' on read-only server fails"() {
        when: "method is called"
        def method = UserServiceImpl.class.getDeclaredMethods().find({ it.getName() == methodName })
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "invocation fails with InvalidWriteOperationException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage()
                    == "UNIMPLEMENTED: Unsupported operation on read-only server"
        })
        0 * observer.onNext(*_)

        where:
        methodName << ["createAccount", "deleteAccount", "transferTo"]
    }

    @Unroll
    def "read operation '#methodName' on read-only server does not fail for invalid write operation"() {
        when: "method is called"
        def method = UserServiceImpl.class.getDeclaredMethods().find({ it.getName() == methodName })
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "invocation does not fail with InvalidWriteOperationException"
        0 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage()
                    == "UNIMPLEMENTED: Unsupported operation on read-only server"
        })
        // might succeed or fail (for an unrelated reason); unknown

        where:
        methodName << ["balance"]
    }
}
