package pt.tecnico.distledger

class CreateAndDeleteAccountIT extends BaseIT {
    def "create account and delete it afterwards"() {
        when: "the user creates an account"
        runUser("createAccount A Alice")

        then: "the output is correct"
        getOutput() == "> OK\n\n> "

        when: "the user deletes the account"
        runUser("deleteAccount A Alice")

        then: "the output is correct"
        getOutput() == "> OK\n\n> "
    }

    def "create duplicate account"() {
        when: "the user creates a duplicate account"
        runUser("createAccount A broker")

        then: "the output is correct"
        getOutput() == "> Error: Account for user broker already exists\n\n> "
    }

    def "create account with inactive server, reactivate and try again"() {
        when: "the server is deactivated"
        runAdmin("deactivate A")

        then: "the output is correct"
        getOutput() == "> OK\n\n> "

        when: "the user creates an account"
        runUser("createAccount A Alice")

        then: "the output is correct"
        getOutput() == "> Error: Server is unavailable\n\n> "

        when: "the server is reactivated"
        runAdmin("activate A")

        then: "the output is correct"
        getOutput() == "> OK\n\n> "

        when: "the user creates an account"
        runUser("createAccount A Alice")

        then: "the output is correct"
        getOutput() == "> OK\n\n> "
    }

    def "delete non-existing account"() {
        when: "the user deletes a non-existing account"
        runUser("deleteAccount A Alice")

        then: "the output is correct"
        getOutput() == "> Error: Account Alice does not exist\n\n> "
    }

    def "delete protected account"() {
        when: "the user deletes a protected account"
        runUser("deleteAccount A broker")

        then: "the output is correct"
        getOutput() == "> Error: Account for user broker is protected\n\n> "
    }

    def "delete non-empty account"() {
        given: "an account with money"
        prepareUser("createAccount A Alice\ntransferTo A broker Alice 1000")

        when: "the user deletes the account"
        runUser("deleteAccount A Alice")

        then: "the output is correct"
        getOutput() == "> Error: Account for user Alice has 1000 left, needs to be empty\n\n> "
    }

    def "delete account with inactive server"() {
        when: "the server is deactivated"
        prepareAdmin("deactivate A")

        and: "the user deletes an account"
        runUser("deleteAccount A broker")

        then: "the output is correct"
        getOutput() == "> Error: Server is unavailable\n\n> "
    }
}