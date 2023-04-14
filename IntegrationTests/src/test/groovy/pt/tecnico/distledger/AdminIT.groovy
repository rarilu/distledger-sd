package pt.tecnico.distledger

class AdminIT extends BaseIT {
    def setup() {
        prepareServers(['A'])
        prepareUsers(1)
    }

    def "admin checks an empty ledger state"() {
        when: "the admin checks the ledger state"
        def output = runAdmin("getLedgerState A")

        then: "the output is correct"
        output == "OK\nledgerState {\n}"

        when: "the admin checks the ledger state again"
        output = runAdmin("getLedgerState A")

        then: "the output is correct"
        output == "OK\nledgerState {\n}"
    }

    def "admin checks a non-empty ledger state"() {
        given: "an account with money"
        runUser("createAccount A Alice")
        runUser("transferTo A broker Alice 1000")
        runUser("transferTo A Alice broker 1000")

        when: "the admin checks the ledger state"
        def output = runAdmin("getLedgerState A")

        then: "the output is correct"
        output == "OK\n" +
                "ledgerState {\n" +
                "  ledger {\n" +
                "    type: OP_CREATE_ACCOUNT\n" +
                "    userId: \"Alice\"\n" +
                "    prevTS {\n" +
                "    }\n" +
                "    TS {\n" +
                "      values: 1\n" +
                "    }\n" +
                "    stable: true\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"broker\"\n" +
                "    destUserId: \"Alice\"\n" +
                "    amount: 1000\n" +
                "    prevTS {\n" +
                "      values: 1\n" +
                "    }\n" +
                "    TS {\n" +
                "      values: 2\n" +
                "    }\n" +
                "    stable: true\n" +
                "  }\n" +
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"Alice\"\n" +
                "    destUserId: \"broker\"\n" +
                "    amount: 1000\n" +
                "    prevTS {\n" +
                "      values: 2\n" +
                "    }\n" +
                "    TS {\n" +
                "      values: 3\n" +
                "    }\n" +
                "    stable: true\n" +
                "  }\n" +
                "}"
    }
}
