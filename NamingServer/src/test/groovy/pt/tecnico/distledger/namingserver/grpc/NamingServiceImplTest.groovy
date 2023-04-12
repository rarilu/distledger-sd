package pt.tecnico.distledger.namingserver.grpc

import spock.lang.Specification

import java.util.stream.Collectors
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import pt.tecnico.distledger.namingserver.domain.NamingServerState
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteRequest
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteResponse
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupRequest
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupResponse
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterRequest
import pt.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterResponse

class NamingServiceImplTest extends Specification {
    def state
    def service
    def observer

    def setup() {
        state = new NamingServerState()
        service = new NamingServiceImpl(state)
        observer = Mock(StreamObserver)
    }

    def "register servers"() {
        when: "a server is registered"
        service.register(RegisterRequest.newBuilder()
                .setService("DistLedger")
                .setQualifier("A")
                .setTarget("localhost:2000")
                .build(),
                observer)

        then: "the correct response is received"
        1 * observer.onNext(RegisterResponse.newBuilder().setAssignedId(0).build())

        when: "another server is registered with the same service"
        service.register(RegisterRequest.newBuilder()
                .setService("DistLedger")
                .setQualifier("B")
                .setTarget("localhost:2001")
                .build(),
                observer)

        then: "the correct response is received"
        1 * observer.onNext(RegisterResponse.newBuilder().setAssignedId(1).build())
    }

    def "register an already registered server"() {
        given: "a server already registered"
        state.registerServer("DistLedger", "A", "localhost:2000")

        when: "the server is registered again"
        service.register(RegisterRequest.newBuilder()
                .setService("DistLedger")
                .setQualifier("A")
                .setTarget("localhost:2000")
                .build(),
                observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException
                    && it.getMessage() ==
                    "ALREADY_EXISTS: " +
                    "An entry for server with target localhost:2000 and service DistLedger already exists"
        })
    }

    def "delete a server"() {
        given: "a server registered"
        state.registerServer("DistLedger", "A", "localhost:2000")

        when: "the server is deleted"
        service.delete(DeleteRequest.newBuilder()
                .setService("DistLedger")
                .setTarget("localhost:2000")
                .build(),
                observer)

        then: "the correct response is received"
        1 * observer.onNext(DeleteResponse.getDefaultInstance())
    }

    def "delete a non-existent server"() {
        when: "a server is deleted"
        service.delete(DeleteRequest.newBuilder()
                .setService("DistLedger")
                .setTarget("localhost:2000")
                .build(),
                observer)

        then: "an exception is thrown"
        1 * observer.onError({
            it instanceof StatusRuntimeException
                    && it.getMessage() ==
                    "NOT_FOUND: An entry for server with target localhost:2000 and service DistLedger was not found"
        })
    }

    def "lookup a server"() {
        given: "a server registered"
        state.registerServer("DistLedger", "A", "localhost:2000")

        when: "the server is looked up"
        service.lookup(LookupRequest.newBuilder().setService(serviceName).setQualifier(qualifier).build(), observer)

        then: "the correct response is received"
        1 * observer.onNext({
            it instanceof LookupResponse
                    && it.getEntriesList().stream().map({ it.getQualifier() }).collect(Collectors.toList()) == qualifiersList
                    && it.getEntriesList().stream().map({ it.getTarget() }).collect(Collectors.toList()) == targetsList
                    && it.getEntriesList().stream().map({ it.getId() }).collect(Collectors.toList()) == idsList
        })

        where:
        serviceName  | qualifier | qualifiersList | targetsList        | idsList
        "DistLedger" | "A"       | ["A"]          | ["localhost:2000"] | [0]
        "DistLedger" | "void"    | []             | []                 | []
        "void"       | "A"       | []             | []                 | []           
    }

    def "lookup all servers in a service"() {
        given: "servers registered"
        state.registerServer("DistLedger", "A", "localhost:2000")
        state.registerServer("DistLedger", "B", "localhost:2001")
        state.registerServer("DistLedger", "C", "localhost:2002")

        when: "the servers are looked up"
        service.lookup(LookupRequest.newBuilder().setService("DistLedger").build(), observer)

        then: "the correct response is received"
        1 * observer.onNext({
            it instanceof LookupResponse
                    && it.getEntriesList() == [
                        LookupResponse.Entry.newBuilder().setQualifier("A").setTarget("localhost:2000").setId(0).build(),
                        LookupResponse.Entry.newBuilder().setQualifier("B").setTarget("localhost:2001").setId(1).build(),
                        LookupResponse.Entry.newBuilder().setQualifier("C").setTarget("localhost:2002").setId(2).build()
                    ]
        })
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

        when: "a method is called"
        method.invoke(service, method.getParameterTypes()[0].getDefaultInstance(), observer)

        then: "method fails with RuntimeException"
        1 * observer.onError({
            it instanceof StatusRuntimeException && it.getMessage() == "UNKNOWN: Unknown error"
        })

        where: "method is any void function of NamingServiceImpl that takes 2 arguments"
        method << NamingServiceImpl.class.getDeclaredMethods().findAll { it.getReturnType() == void.class
                && it.getParameterTypes().size() == 2 }
    }
}
