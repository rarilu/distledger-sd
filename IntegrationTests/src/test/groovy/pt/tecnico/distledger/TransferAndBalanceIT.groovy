package pt.tecnico.distledger

class TransferAndBalanceIT extends BaseIT {
    def setup() {
        prepareServers(['A'])
        prepareUsers(1)
    }

    def "transfer a non-positive amount"() {
        given: "an empty account"
        runUser("createAccount A Alice")

        when: "the user transfers a non-positive amount"
        def output = runUser("transferTo A broker Alice " + amount)

        then: "the output is correct"
        output == "OK"

        when: "the user checks the balance of the account"
        output = runUser("balance A Alice")

        then: "the balance is correct"
        output == "OK\nvalue: 0"

        where:
        amount << [0, -1]
    }

    def "transfer from an unknown account"() {
        when: "the user transfers money from an unknown account"
        def output = runUser("transferTo A Alice broker 1000")

        then: "the output is correct"
        output == "OK"

        when: "the user checks the balance of the broker account"
        output = runUser("balance A broker")

        then: "the balance is correct"
        output == "OK\nvalue: 1000"
    }

    def "transfer to an unknown account"() {
        when: "the user transfers money to an unknown account"
        def output = runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        output == "OK"

        when: "the user checks the balance of the broker account"
        output = runUser("balance A broker")

        then: "the balance is correct"
        output == "OK\nvalue: 1000"
    }

    def "transfer without enough balance"() {
        given: "an account with money"
        runUser("createAccount A Alice\ntransferTo A broker Alice 1000")

        when: "the user transfers more money than they have"
        def output = runUser("transferTo A Alice broker 1001")

        then: "the output is correct"
        output == "OK"

        when: "the user checks the balance of the account"
        output = runUser("balance A Alice")

        then: "the balance is correct"
        output == "OK\nvalue: 1000"
    }

    def "transfer to same account"() {
        when: "the user transfers money to the same account"
        def output = runUser("transferTo A broker broker 1000")

        then: "the output is correct"
        output == "OK"

        when: "the user checks the balance of the account"
        output = runUser("balance A broker")

        then: "the balance is correct"
        output == "OK\nvalue: 1000"
    }

    def "transfer with inactive server"() {
        when: "the server is deactivated"
        runAdmin("deactivate A")

        and: "the user transfers money"
        def output = runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        output == "Error: Server is unavailable"
    }

    def "balance of an account"() {
        when: "the user checks the balance of an account"
        def output = runUser("balance A broker")

        then: "the output is correct"
        output == "OK\nvalue: 1000"
    }

    def "balance of a non-existing account"() {
        when: "the user checks the balance of a non-existing account"
        def output = runUser("balance A Alice")

        then: "the output is correct"
        output == "Error: NOT_FOUND: Account Alice does not exist"
    }

    def "balance of an account with inactive server"() {
        when: "the server is deactivated"
        runAdmin("deactivate A")
        
        and: "the user checks the balance of an account"
        def output = runUser("balance A broker")

        then: "the output is correct"
        output == "Error: Server is unavailable"
    }
}
