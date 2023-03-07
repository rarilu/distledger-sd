package pt.tecnico.distledger.userclient

import org.grpcmock.GrpcMock
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc

class UserClientMainTest extends BaseTest {
    def "main is called with no arguments"() {
        when: "the user client is called with no arguments"
        def userClient = new UserClientMain() // main is static, but this is needed to cover the constructor
        userClient.main(new String[]{})

        then: "there is no output"
        outBuf.toString() == ""
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
