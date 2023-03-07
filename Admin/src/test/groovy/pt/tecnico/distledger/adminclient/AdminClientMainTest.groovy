package pt.tecnico.distledger.adminclient

import org.grpcmock.GrpcMock
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc

class AdminClientMainTest extends BaseTest {
    def "main is called with no arguments"() {
        when: "the admin client is run with no arguments"
        def adminClient = new AdminClientMain() // main is static, but this is needed to cover the constructor
        adminClient.main(new String[]{})

        then: "the output is correct"
        errBuf.toString() == "Argument(s) missing!\nUsage: mvn exec:java -Dexec.args=<host> <port>\n"
    }

    def "user provides an unknown command"() {
        given: "an unknown command input"
        provideInput("thisCommandDoesNotExist wrong arguments\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == EXPECTED_USAGE_STRING
    }
}
