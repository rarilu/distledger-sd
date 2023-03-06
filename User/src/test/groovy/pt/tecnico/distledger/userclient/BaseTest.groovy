package pt.tecnico.distledger.userclient

import spock.lang.Specification

import pt.tecnico.distledger.userclient.grpc.UserService;
import org.grpcmock.GrpcMock;

abstract class BaseTest extends Specification {
    def userService
    def commandParser

    def initialStdin
    def initialStdout
    def outBuf

    def setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start())
        // port 0 means that the OS will assign a random free port

        userService = new UserService("localhost", GrpcMock.getGlobalPort())
        commandParser = new CommandParser(userService)

        initialStdin = System.in
        initialStdout = System.out
        outBuf = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuf))
    }

    def cleanup() {
        userService.close()
        System.setIn(initialStdin)
        System.setOut(initialStdout)
    }

    def provideInput(String input) {
        ByteArrayInputStream mockStdin = new ByteArrayInputStream(input.getBytes())
        System.setIn(mockStdin)
    }
}
