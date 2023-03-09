package pt.tecnico.distledger.server.domain

import spock.lang.Specification

class ServerStateTest extends Specification {
    def "state has broker account with 1000"() {
        when: "a new server state is created"
        def state = new ServerState()

        then: "exactly one account exists"
        state.getAccounts().size() == 1        

        and: "the broker account has the correct balance"
        state.getAccounts().get("broker").getBalance() == 1000
    }
}
