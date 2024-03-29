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

    def "register a server, delete it, and register it again"() {
        when: "a server is registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        then: "the lookup returns the server"
        namingServerState.lookup("DistLedger", "A") ==
                Optional.of(new ServerEntry("A", "localhost:8000", 0))

        when: "the server is deleted"
        namingServerState.deleteServer("DistLedger", "localhost:8000")

        then: "the lookup returns no servers"
        namingServerState.lookup("DistLedger", "A").isEmpty()

        when: "the server is registered again"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        then: "the lookup returns the server"
        namingServerState.lookup("DistLedger", "A") ==
                Optional.of(new ServerEntry("A", "localhost:8000", 1))
    }

    def "register a server with multiple qualifiers"() {
        when: "a server is registered with multiple qualifiers"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")
        namingServerState.registerServer("DistLedger", "B", "localhost:8000")

        then: "an exception is thrown"
        thrown(DuplicateServerEntryException)
    }

    def "register a server with multiple services"() {
        given: "a server registered on multiple services"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")
        namingServerState.registerServer("Other", "A", "localhost:8000")

        when: "a lookup for DistLedger is performed"
        def distledger = namingServerState.lookup("DistLedger")

        and: "a lookup for Other is performed"
        def other = namingServerState.lookup("Other")

        then: "the lookups return the server"
        distledger == [new ServerEntry("A", "localhost:8000", 0)]
        other == [new ServerEntry("A", "localhost:8000", 0)]
    }

    def "delete a server with a non-existent service"() {
        when: "a non-existent server is deleted"
        namingServerState.deleteServer("DistLedger", "localhost:8000")

        then: "an exception is thrown"
        thrown(ServerEntryNotFoundException)
    }

    def "delete a non-existent server"() {
        given: "a server already registered"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")

        when: "a non-existent server with the same service is deleted"
        namingServerState.deleteServer("DistLedger", "localhost:8001")

        then: "an exception is thrown"
        thrown(ServerEntryNotFoundException)
    }

    def "lookup multiple servers"() {
        given: "multiple servers already registered on one service"
        namingServerState.registerServer("DistLedger", "A", "localhost:8000")
        namingServerState.registerServer("DistLedger", "B", "localhost:8001")
        namingServerState.registerServer("DistLedger", "C", "localhost:8002")

        and: "multiple servers already registered on another service"
        namingServerState.registerServer("Other", "A", "localhost:8005")
        namingServerState.registerServer("Other", "B", "localhost:8006")

        when: "a lookup for DistLedger is performed"
        def distledger = namingServerState.lookup("DistLedger")

        and: "a lookup for Distledger A is performed"
        def distledgerA = namingServerState.lookup("DistLedger", "A")

        and: "a lookup for Other is performed"
        def other = namingServerState.lookup("Other")

        and: "two lookups which should return no servers are performed"
        def none = namingServerState.lookup("None")
        def distledgerD = namingServerState.lookup("DistLedger", "D")

        then: "the lookup returns the correct servers"
        distledger == [new ServerEntry("A", "localhost:8000", 0),
                       new ServerEntry("B", "localhost:8001", 1),
                       new ServerEntry("C", "localhost:8002", 2)]
        distledgerA == Optional.of(new ServerEntry("A", "localhost:8000", 0))
        other == [new ServerEntry("A", "localhost:8005", 0),
                  new ServerEntry("B", "localhost:8006", 1)]
        none.isEmpty()
        distledgerD.isEmpty()
    }
}
