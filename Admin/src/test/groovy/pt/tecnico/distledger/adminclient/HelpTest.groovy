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
}
