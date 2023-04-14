package pt.tecnico.distledger

class CreateAccountIT extends BaseIT {
    def setup() {
        prepareServers(['A'])
        prepareUsers(1)
    }

    def "create account"() {
        when: "the user creates an account"
        def output = runUser("createAccount A Alice")

        then: "the output is correct"
        output == "OK"
    }

    def "create duplicate account"() {
        when: "the user creates a duplicate account"
        def output = runUser("createAccount A broker")

        then: "the output is correct"
        output == "OK"
    }

    def "create account with inactive server, reactivate and try again"() {
        when: "the server is deactivated"
        def output = runAdmin("deactivate A")

        then: "the output is correct"
        output == "OK"

        when: "the user creates an account"
        output = runUser("createAccount A Alice")

        then: "the output is correct"
        output == "Error: Server is unavailable"

        when: "the server is reactivated"
        output = runAdmin("activate A")

        then: "the output is correct"
        output == "OK"

        when: "the user creates an account"
        output = runUser("createAccount A Alice")

        then: "the output is correct"
        output == "OK"
    }
}
