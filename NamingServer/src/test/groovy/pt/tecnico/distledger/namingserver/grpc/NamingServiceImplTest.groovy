package pt.tecnico.distledger.namingserver.grpc

import spock.lang.Specification

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import pt.tecnico.distledger.namingserver.domain.NamingServerState
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ShutdownRequest
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ShutdownResponse

class NamingServiceImplTest extends Specification {
    def service
    def observer

    def setup() {
        def state = new NamingServerState()
        service = new NamingServiceImpl(state)
        observer = Mock(StreamObserver)
    }

    def "try to shutdown without passing the server"() {
        when: "shutdown is called"
        service.shutdown(ShutdownRequest.getDefaultInstance(), observer)

        then: "method fails with UNIMPLEMENTED"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNIMPLEMENTED: Server cannot be shutdown"
        })
    }

    def "shutdown successfully"() {
        given: "a mock server which is passed to the service"
        def server = Mock(io.grpc.Server)
        service.setServer(server)

        when: "shutdown is called"
        service.shutdown(ShutdownRequest.getDefaultInstance(), observer)

        then: "server is shutdown"
        1 * server.shutdown()

        and: "response is sent"
        1 * observer.onNext(ShutdownResponse.getDefaultInstance())
    }

    def "catch runtime exceptions"() {
        given: "a mock state that throws an exception when a method is called"
        def state = Mock(NamingServerState)
        state.registerServer(_) >> { throw new RuntimeException("Unknown error") }
        state.deleteServer(_) >> { throw new RuntimeException("Unknown error") }
        state.lookup(*_) >> { throw new RuntimeException("Unknown error") }

        and: "an observer that throws an exception when onNext is called"
        observer.onNext(_) >> { throw new RuntimeException("Unknown error") }

        and: "a service that uses the mock state"
        service = new NamingServiceImpl(state)

        and: "a mock server which is passed to the service"
        service.setServer(Mock(io.grpc.Server))

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any void function of NamingServiceImpl that takes 2 arguments"
        method << NamingServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class && it.getParameterTypes().size() == 2 }
    }
}
