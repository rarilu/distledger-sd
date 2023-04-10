package pt.tecnico.distledger

class CreateAccountIT extends BaseIT {
    def "create account"() {
        when: "the user creates an account"
        runUser("createAccount A Alice")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "
    }

    def "create duplicate account"() {
        when: "the user creates a duplicate account"
        runUser("createAccount A broker")

        then: "the output is correct"
        extractOutput() == "> Error: ALREADY_EXISTS: Account for user broker already exists\n\n> "
    }

    def "create account with inactive server, reactivate and try again"() {
        when: "the server is deactivated"
        runAdmin("deactivate A")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user creates an account"
        runUser("createAccount A Alice")

        then: "the output is correct"
        extractOutput() == "> Error: Server is unavailable\n\n> "

        when: "the server is reactivated"
        runAdmin("activate A")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "

        when: "the user creates an account"
        runUser("createAccount A Alice")

        then: "the output is correct"
        extractOutput() == "> OK\n\n> "
    }
}
