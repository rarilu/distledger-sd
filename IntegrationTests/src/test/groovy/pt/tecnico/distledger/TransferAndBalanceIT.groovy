package pt.tecnico.distledger

class TransferAndBalanceIT extends BaseIT {
    def "transfer a non-positive amount"() {
        given: "an empty account"
        prepareUser("createAccount A Alice")

        when: "the user transfers a non-positive amount"
        runUser("transferTo A broker Alice " + amount)

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user checks the balance of the account"
        runUser("balance A Alice")

        then: "the balance is correct"
        extractOutput() == "> OK\nvalue: 0\n\n> "

        where:
        amount << [0, -1]
    }

    def "transfer from an unknown account"() {
        when: "the user transfers money from an unknown account"
        runUser("transferTo A Alice broker 1000")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user checks the balance of the broker account"
        runUser("balance A broker")

        then: "the balance is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "
    }

    def "transfer to an unknown account"() {
        when: "the user transfers money to an unknown account"
        runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user checks the balance of the broker account"
        runUser("balance A broker")

        then: "the balance is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "
    }

    def "transfer without enough balance"() {
        given: "an account with money"
        prepareUser("createAccount A Alice\ntransferTo A broker Alice 1000")

        when: "the user transfers more money than they have"
        runUser("transferTo A Alice broker 1001")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user checks the balance of the account"
        runUser("balance A Alice")

        then: "the balance is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "
    }

    def "transfer to same account"() {
        when: "the user transfers money to the same account"
        runUser("transferTo A broker broker 1000")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user checks the balance of the account"
        runUser("balance A broker")

        then: "the balance is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "
    }

    def "transfer with inactive server"() {
        when: "the server is deactivated"
        prepareAdmin("deactivate A")

        and: "the user transfers money"
        runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        extractOutput() == "> Error: Server is unavailable\n\n> "
    }

    def "balance of an account"() {
        when: "the user checks the balance of an account"
        runUser("balance A broker")

        then: "the output is correct"
        extractOutput() == "> OK\nvalue: 1000\n\n> "
    }

    def "balance of a non-existing account"() {
        when: "the user checks the balance of a non-existing account"
        runUser("balance A Alice")

        then: "the output is correct"
        extractOutput() == "> Error: NOT_FOUND: Account Alice does not exist\n\n> "
    }

    def "balance of an account with inactive server"() {
        when: "the server is deactivated"
        prepareAdmin("deactivate A")
        
        and: "the user checks the balance of an account"
        runUser("balance A broker")

        then: "the output is correct"
        extractOutput() == "> Error: Server is unavailable\n\n> "
    }
}
