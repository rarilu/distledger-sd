package pt.tecnico.distledger.namingserver.domain

import pt.tecnico.distledger.namingserver.domain.exceptions.DuplicateServerEntryException
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
}
