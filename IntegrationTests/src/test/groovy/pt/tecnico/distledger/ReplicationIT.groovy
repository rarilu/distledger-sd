package pt.tecnico.distledger

class ReplicationIT extends BaseIT {
    def "state can be read in the secondary server even if the primary server is inactive"() {
        given: "an account created in the primary server"
        prepareUser("createAccount A Alice")

        and: "a transfer made in the primary server"
        prepareUser("transferTo A broker Alice 1000")

        and: "the primary server is deactivated"
        prepareAdmin("deactivate A")

        when: "the user checks the balance on the secondary server"
        runUser("balance B Alice")

        then: "the output is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "

        when: "the admin checks the ledger on the secondary server"
        runAdmin("getLedgerState B")

        then: "the output is correct"
        extractOutput() == "> OK\n" +
                "ledgerState {\n" +
                "  ledger {\n" +
                "    type: OP_CREATE_ACCOUNT\n" +
                "    userId: \"Alice\"\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"broker\"\n" +
                "    destUserId: \"Alice\"\n" +
                "    amount: 1000\n" +
                "  }\n" +
                "}\n\n> "
    }

    def "state cannot be written to the secondary server"() {
        when: "the user creates an account on the secondary server"
        runUser("createAccount B Alice")

        then: "the output is correct"
        extractOutput() == "> Error: UNIMPLEMENTED: Unsupported operation on read-only server\n\n> "

        when: "the user transfers money on the secondary server"
        runUser("transferTo B broker Alice 1000")

        then: "the output is correct"
        extractOutput() == "> Error: UNIMPLEMENTED: Unsupported operation on read-only server\n\n> "

        when: "the user deletes an account on the secondary server"
        runUser("deleteAccount B Alice")

        then: "the output is correct"
        extractOutput() == "> Error: UNIMPLEMENTED: Unsupported operation on read-only server\n\n> "
    }

    def "state can be read in the primary server even if the secondary server is inactive"() {
        given: "the secondary server is deactivated"
        prepareAdmin("deactivate B")

        when: "the user checks the balance on the primary server"
        runUser("balance A broker")

        then: "the output is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "
        
    }

    def "state cannot be written to the primary server if the secondary server is inactive"() {
        given: "an account created"
        prepareUser("createAccount A Alice")

        and: "the secondary server is deactivated"
        prepareAdmin("deactivate B")

        when: "the user creates an account on the primary server"
        runUser("createAccount A Bob")

        then: "the output is correct"
        extractOutput() == "> Error: ABORTED: pt.tecnico.distledger.server.domain.exceptions.FailedPropagationException: Secondary server is unavailable\n\n> "
    
        when: "the user transfers money on the primary server"
        runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        extractOutput() == "> Error: ABORTED: pt.tecnico.distledger.server.domain.exceptions.FailedPropagationException: Secondary server is unavailable\n\n> "

        when: "the user deletes an account on the primary server"
        runUser("deleteAccount A Alice")

        then: "the output is correct"
        extractOutput() == "> Error: ABORTED: pt.tecnico.distledger.server.domain.exceptions.FailedPropagationException: Secondary server is unavailable\n\n> "
    }
}
