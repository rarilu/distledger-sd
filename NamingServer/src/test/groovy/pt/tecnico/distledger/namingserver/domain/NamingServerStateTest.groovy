package pt.tecnico.distledger.namingserver.domain

import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException
import pt.tecnico.distledger.namingserver.domain.exceptions.ServerEntryNotFoundException
import spock.lang.Specification

class NamingServerStateTest extends Specification {
    def namingServerState

    def setup() {
        namingServerState = new NamingServerState()
    }

    def "registering a duplicate server throws exception"() {
        given: "a server already registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        when: "a server with the same details is registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        then: "an exception is thrown"
        thrown(DuplicateServerEntryException)
    }

    def "register a server"() {
        when: "a server is registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        then: "the lookup returns the server"
        namingServerState.lookupServer("DistLedger", "A") == ["localhost:8000"]
    }

    def "delete a server"() {
        given: "a server already registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        when: "the server is deleted"
        namingServerState.deleteServer("DistLedger", "localhost:8000")

        then: "the lookup returns no servers"
        namingServerState.lookupServer("DistLedger", "A").isEmpty()
    }

    def "delete a server with a non-existant service"() {
        when: "a non-existant server is deleted"
        namingServerState.deleteServer("DistLedger", "localhost:8000")

        then: "an exception is thrown"
        thrown(ServerEntryNotFoundException)
    }

    def "delete a non-existant server"() {
        given: "a server already registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        when: "a non-existant server with the same service is deleted"
        namingServerState.deleteServer("DistLedger", "localhost:8001")

        then: "an exception is thrown"
        thrown(ServerEntryNotFoundException)
    }

    def "lookup multiple servers"() {
        when: "multiple servers are registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")
        namingServerState.registerServer("DistLedger", "A", "localhost:8001")
        namingServerState.registerServer("DistLedger", "A", "localhost:8002")

        then: "the lookup returns the servers"
        namingServerState.lookupServer("DistLedger", "A") == ["localhost:8000", "localhost:8001", "localhost:8002"]
    }
}
