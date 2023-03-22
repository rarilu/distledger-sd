package pt.tecnico.distledger.namingserver

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
}
