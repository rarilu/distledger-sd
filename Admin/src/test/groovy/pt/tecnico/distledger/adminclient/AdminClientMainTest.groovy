package pt.tecnico.distledger.adminclient

class AdminClientMainTest extends BaseTest {
    def "main is called with no arguments"() {
        when: "the admin client is run with no arguments"
        def adminClient = new AdminClientMain() // main is static, but this is needed to cover the constructor
        adminClient.main(new String[]{})

        then: "the output is correct"
        errBuf.toString() == "Argument(s) missing!\nUsage: mvn exec:java -Dexec.args=<host> <port>\n"
    }
}