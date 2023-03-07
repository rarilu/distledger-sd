package pt.tecnico.distledger.userclient

class UserClientMainTest extends BaseTest {
    def "main is called with no arguments"() {
        when: "the user client is called with no arguments"
        def userClient = new UserClientMain() // main is static, but this is needed to cover the constructor
        userClient.main(new String[]{})

        then: "there is no output"
        outBuf.toString() == ""

        and: "an error is logged"
        errBuf.toString() == "Argument(s) missing!\nUsage: mvn exec:java -Dexec.args=<host> <port>\n"
    }
}
