package pt.tecnico.distledger

class ReplicationIT extends BaseIT {
    def output

    def setup() {
        prepareServers(['A', 'B', 'C'])
        prepareUsers(2)
    }

    def "outdated replica"() {
        given: "a user creates an account on replica A"
        runUser("createAccount A Alice")

        when: "we get the user's timestamp"
        output = runUser("timestamp")

        then: "the timestamp is (1)"
        output == "OK\ntimestamp: (1)"

        when: "the user blocks on reading the account's balance on replica B"
        dispatchUser("balance B Alice")

        and: "replica A gossips"
        runAdmin("gossip A")

        then: "the output is correct"
        waitUser() == "OK\nvalue: 0"

        when: "we check the ledger's state on replica B"
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
        given: "a user creates an account on replica A"
        runUser("createAccount A Alice")

        when: "the user makes a transfer to that account on replica B"
        output = runUser("transferTo B broker Alice 100")

        then: "the output is correct"
        output == "OK"

        when: "we check the ledger's state on replica B"
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
                "      values: 1\n" +
                "      values: 1\n" +
                "    }\n" +
                "  }\n" +
                "}"

        when: "the user blocks on reading the account's balance on replica B"
        dispatchUser("balance B Alice")

        and: "replica A gossips"
        runAdmin("gossip A")

        then: "the output is correct"
        waitUser() == "OK\nvalue: 100"

        when: "we check the ledger's state on replica B"
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
                "      values: 1\n" +
                "      values: 1\n" +
                "    }\n" +
                "    stable: true\n" +
                "  }\n" +
                "}"

        when: "the user blocks on reading the account's balance on replica A"
        dispatchUser("balance A Alice")

        and: "B gossips"
        runAdmin("gossip B")

        then: "the output is correct"
        waitUser() == "OK\nvalue: 100"
    }

    def "two users make transfers to each other"() {
        given: "an initial state with two accounts"
        runUser(0, "createAccount A Alice")
        runUser(0, "transferTo A broker Alice 100")
        runUser(0, "createAccount A Bob")
        runUser(0, "transferTo A broker Bob 100")
        runAdmin("gossip A")

        when: "the first user makes a transfer to the second user in replica A"
        runUser(0, "transferTo A Alice Bob 50")

        and: "the second user makes a transfer to the first user in replica B"
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

    def "alice bob barbara charlie"() {
        given: "a user creates one account on replica A"
        runUser("createAccount A Alice")

        and: "the user creates two accounts on replica B"
        runUser("createAccount B Bob")
        runUser("createAccount B Barbara")

        and: "the user creates one account on replica C"
        runUser("createAccount C Charlie")

        when: "replica B gossips"
        runAdmin("gossip B")

        then: "the ledger's state on replica A is correct"
        runAdmin("getLedgerState A") == "OK\n" +
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
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Bob\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 1\n" +
            "    }\n" +
            "    stable: true\n" +
            "  }\n" +
            "  ledger {\n" +
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Barbara\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "      values: 1\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "    }\n" +
            "    stable: true\n" +
            "  }\n" +
            "}"

        and: "the ledger's state on replica C is correct"
        runAdmin("getLedgerState C") == "OK\n" +
            "ledgerState {\n" +
            "  ledger {\n" +
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Charlie\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "      values: 1\n" +
            "    }\n" +
            "  }\n" +
            "  ledger {\n" +
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Bob\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 1\n" +
            "    }\n" +
            "  }\n" +
            "  ledger {\n" +
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Barbara\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "      values: 1\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "    }\n" +
            "  }\n" +
            "}"
        
        when: "replica A gossips"
        runAdmin("gossip A")

        then: "the ledger's state on replica C is correct"
        runAdmin("getLedgerState C") == "OK\n" +
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
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Bob\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 1\n" +
            "    }\n" +
            "    stable: true\n" +
            "  }\n" +
            "  ledger {\n" +
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Barbara\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "      values: 1\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "    }\n" +
            "    stable: true\n" +
            "  }\n" +
            "  ledger {\n" +
            "    type: OP_CREATE_ACCOUNT\n" +
            "    userId: \"Charlie\"\n" +
            "    prevTS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "    }\n" +
            "    TS {\n" +
            "      values: 1\n" +
            "      values: 2\n" +
            "      values: 1\n" +
            "    }\n" +
            "    stable: true\n" +
            "  }\n" +
            "}"
    }
}
