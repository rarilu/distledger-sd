package pt.tecnico.distledger

class AdminIT extends BaseIT {
    def "admin checks an empty ledger state"() {
        when: "the admin checks the ledger state"
        runAdmin("getLedgerState A")

        then: "the output is correct"
        extractOutput() == "> OK\nledgerState {\n}\n\n> "
    }

    def "admin checks a non-empty ledger state"() {
        given: "an account with money"
        prepareUser("createAccount A Alice\ntransferTo A broker Alice 1000")
        prepareUser("transferTo A Alice broker 1000\ndeleteAccount A Alice")

        when: "the admin checks the ledger state"
        runAdmin("getLedgerState A")

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
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"Alice\"\n" +
                "    destUserId: \"broker\"\n" +
                "    amount: 1000\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_DELETE_ACCOUNT\n" +
                "    userId: \"Alice\"\n" +
                "  }\n" +
                "}\n\n> "
    }
}
