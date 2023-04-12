package pt.tecnico.distledger.userclient

class HelpTest extends BaseTest {
    def EXPECTED_HELP_STRING = "Usage:\n" +
        "- createAccount <server> <username>\n" +
        "- balance <server> <username>\n" +
        "- transferTo <server> <username_from> <username_to> <amount>\n" +
        "- timestamp\n" +
        "- exit\n"

    def "help output is correct"() {
        given: "a help command"
        provideInput("help")

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_HELP_STRING + "\n> ")
    }

    def "user provides an unknown command"() {
        given: "an unknown command input"
        provideInput("thisCommandDoesNotExist wrong arguments\nexit\n")

        when: "the user client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == ("> " + EXPECTED_USAGE_STRING + "\n> ")
    }
}
