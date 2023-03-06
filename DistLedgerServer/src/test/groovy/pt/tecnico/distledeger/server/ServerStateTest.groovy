import spock.lang.Specification

import pt.tecnico.distledger.server.domain.ServerState

class ServerStateTest extends Specification {
    def "state has broker account with 1000"() {
        when:
        def state = new ServerState()

        then:
        state.getAccounts().size() == 1
        state.getAccounts().get("broker") == 1000
    }
}
