package pt.tecnico.distledger.server

import pt.tecnico.distledger.userclient.UserClientMain
import pt.tecnico.distledger.adminclient.AdminClientMain

import spock.lang.Specification

class ServerIT extends Specification {
    def port = 2001

    def initialStdin
    def initialStdout
    def outBuf

    def serverThread

    def setup() {
        initialStdin = System.in
        initialStdout = System.out

        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))

        serverThread = Thread.start {
            ServerMain.main(new String[] { port.toString(), "A" })
        }

        // hacky way to wait for the server to start
        // needed to prevent the initial server messages from being read by the tests
        prepareAdmin("getLedgerState A")
    }

    def cleanup() {
        serverThread.interrupt()
        serverThread.join()

        System.setIn(initialStdin)
        System.setOut(initialStdout)
    }

    def prepareUser(String input) {
        runUser(input)
        outBuf.reset()
    }

    def prepareAdmin(String input) {
        runAdmin(input)
        outBuf.reset()
    }

    def runUser(String input) {
        provideInput(input)
        UserClientMain.main(new String[]{"localhost", port.toString()})
    }

    def runAdmin(String input) {
        provideInput(input)
        AdminClientMain.main(new String[]{"localhost", port.toString()})
    }

    def provideInput(String input) {
        ByteArrayInputStream mockStdin = new ByteArrayInputStream(input.getBytes())
        System.setIn(mockStdin)
    }

    def getOutput() {
        def str = outBuf.toString()
        outBuf.reset()
        return str
    }

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

    def "transfer a non-positive amount"() {
        given: "an empty account"
        prepareUser("createAccount A Alice")

        when: "the user transfers a non-positive amount"
        runUser("transferTo A broker Alice " + amount)

        then: "the output is correct"
        getOutput() == "> Error: Transfers with non-positive amount are not allowed\n\n> "

        where:
        amount << [0, -1]
    }

    def "transfer from an unknown account"() {
        when: "the user transfers money from an unknown account"
        runUser("transferTo A Alice broker 1000")

        then: "the output is correct"
        getOutput() == "> Error: Account Alice does not exist\n\n> "
    }

    def "transfer to an unknown account"() {
        when: "the user transfers money to an unknown account"
        runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        getOutput() == "> Error: Account Alice does not exist\n\n> "
    }

    def "transfer without enough balance"() {
        given: "an account with money"
        prepareUser("createAccount A Alice\ntransferTo A broker Alice 1000")

        when: "the user transfers more money than they have"
        runUser("transferTo A Alice broker 1001")

        then: "the output is correct"
        getOutput() == "> Error: Account Alice does not have enough balance to transfer 1001\n\n> "
    }

    def "transfer to same account"() {
        when: "the user transfers money to the same account"
        runUser("transferTo A broker broker 1000")

        then: "the output is correct"
        getOutput() == "> Error: Transfers from an account to itself are not allowed\n\n> "
    }

    def "transfer with inactive server"() {
        when: "the server is deactivated"
        prepareAdmin("deactivate A")

        and: "the user transfers money"
        runUser("transferTo A broker Alice 1000")

        then: "the output is correct"
        getOutput() == "> Error: Server is unavailable\n\n> "
    }

    def "balance of an account"() {
        when: "the user checks the balance of an account"
        runUser("balance A broker")

        then: "the output is correct"
        getOutput() == "> OK\nvalue: 1000\n\n> "
    }

    def "balance of a non-existing account"() {
        when: "the user checks the balance of a non-existing account"
        runUser("balance A Alice")

        then: "the output is correct"
        getOutput() == "> Error: Account Alice does not exist\n\n> "
    }

    def "balance of an account with inactive server"() {
        when: "the server is deactivated"
        prepareAdmin("deactivate A")
        
        and: "the user checks the balance of an account"
        runUser("balance A broker")

        then: "the output is correct"
        getOutput() == "> Error: Server is unavailable\n\n> "
    }

    def "admin checks an empty ledger state"() {
        when: "the admin checks the ledger state"
        runAdmin("getLedgerState A")

        then: "the output is correct"
        getOutput() == "> OK\nledgerState {\n}\n\n> "
    }

    def "admin checks a non-empty ledger state"() {
        given: "an account with money"
        prepareUser("createAccount A Alice\ntransferTo A broker Alice 1000")
        prepareUser("transferTo A Alice broker 1000\ndeleteAccount A Alice")

        when: "the admin checks the ledger state"
        runAdmin("getLedgerState A")

        then: "the output is correct"
        getOutput() == "> OK\n" +
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
