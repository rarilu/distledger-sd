package pt.tecnico.distledger

class ReplicationIT extends BaseIT {
    def output

    def setup() {
        prepareServers(['A', 'B'])
        prepareUsers(2)
    }

    def "outdated replica"() {
        given: "an user creates an account on A"
        runUser("createAccount A Alice")

        when: "we get the user's timestamp"
        output = runUser("timestamp")

        then: "the timestamp is (1)"
        output == "OK\ntimestamp: (1)"

        when: "the user tries reading the account's balance on B"
        output = runUser("balance B Alice")

        then: "the query fails because the replica is outdated"
        output == "Error: UNKNOWN: Request has timestamp (1), but the state timestamp is still ()"

        when: "A gossips"
        runAdmin("gossip A")

        and: "the user tries reading the account's balance on B again"
        output = runUser("balance B Alice")

        then: "the query succeeds"
        output == "OK\nvalue: 0"

        when: "we check the ledger's state on B"
        output = runAdmin("getLedgerState B")

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
                "}"
    }

    def "operation is only executed when prevTS >= valueTS"() {
        given: "an user creates an account on A"
        runUser("createAccount A Alice")

        when: "the user transfers money to that account on B"
        output = runUser("transferTo B broker Alice 100")

        then: "the output is correct"
        output == "OK"

        when: "we check the ledger's state on B"
        output = runAdmin("getLedgerState B")

        then: "the output is correct"
        output == "OK\n" +
                "ledgerState {\n" +
                "  ledger {\n" +
                "    type: OP_TRANSFER_TO\n" +
                "    userId: \"broker\"\n" +
                "    destUserId: \"Alice\"\n" +
                "    amount: 100\n" +
                "    prevTS {\n" +
                "      values: 1\n" +
                "    }\n" +
                "    TS {\n" +
                "      values: 0\n" +
                "      values: 1\n" +
                "    }\n" +
                "  }\n" +
                "}"

        when: "the user tries reading the account's balance on B"
        output = runUser("balance B Alice")

        then: "the query fails because the replica is outdated"
        output == "Error: UNKNOWN: Request has timestamp (1, 1), but the state timestamp is still ()"

        when: "A gossips"
        runAdmin("gossip A")

        and: "the user tries reading the account's balance on B again"
        output = runUser("balance B Alice")

        then: "the query succeeds"
        output == "OK\nvalue: 100"

        when: "we check the ledger's state on B"
        output = runAdmin("getLedgerState B")

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
                "    amount: 100\n" +
                "    prevTS {\n" +
                "      values: 1\n" +
                "    }\n" +
                "    TS {\n" +
                "      values: 0\n" +
                "      values: 1\n" +
                "    }\n" +
                "    stable: true\n" +
                "  }\n" +
                "}"

        when: "the user tries reading the account's balance on A"
        output = runUser("balance A Alice")

        then: "the query fails because the replica is outdated"
        output == "Error: UNKNOWN: Request has timestamp (1, 1), but the state timestamp is still (1)"

        when: "B gossips"
        runAdmin("gossip B")

        and: "the user tries reading the account's balance on A again"
        output = runUser("balance A Alice")

        then: "the query succeeds"
        output == "OK\nvalue: 100"
    }

    def "two users transfer money to each other"() {
        given: "an initial state with two accounts"
        runUser(0, "createAccount A Alice")
        runUser(0, "transferTo A broker Alice 100")
        runUser(0, "createAccount A Bob")
        runUser(0, "transferTo A broker Bob 100")
        runAdmin("gossip A")

        when: "the first user transfers money to the second user in A"
        runUser(0, "transferTo A Alice Bob 50")

        and: "the second user transfers money to the first user in B"
        runUser(1, "transferTo B Bob Alice 50")

        then: "the first user's balance is correct"
        runUser(0, "balance A Alice") == "OK\nvalue: 50"

        and: "the second user's balance is correct"
        runUser(1, "balance B Bob") == "OK\nvalue: 50"

        when: "both replicas gossip"
        runAdmin("gossip A")
        runAdmin("gossip B")

        then: "the first user's balance is correct"
        runUser(0, "balance A Alice") == "OK\nvalue: 100"

        and: "the second user's balance is correct"
        runUser(1, "balance B Bob") == "OK\nvalue: 100"
    }
}
