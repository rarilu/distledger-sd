package pt.tecnico.distledger.adminclient

class HelpTest extends BaseTest {
    def EXPECTED_HELP_STRING = "Usage:\n" +
        "- activate <server>\n" +
        "- deactivate <server>\n" +
        "- getLedgerState <server>\n" +
        "- gossip <server>\n" +
        "- shutdown <server>\n" +
        "- exit\n"

    def "help output is correct"() {
        given: "a help command"
        provideInput("help")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_HELP_STRING + "\n> ")
    }

    def "user provides an unknown command"() {
        given: "an unknown command input"
        provideInput("thisCommandDoesNotExist wrong arguments\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")
    }
}
