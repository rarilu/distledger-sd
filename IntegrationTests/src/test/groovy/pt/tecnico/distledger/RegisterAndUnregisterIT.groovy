package pt.tecnico.distledger

class RegisterAndUnregisterIT extends BaseIT {
    def "fail to unregister the server"() {
        given: "a server is running"
        prepareServers(['A'])

        and: "the naming server is stopped"
        stopNamingServer()

        when: "the server tries to unregister"
        def output = stopServer(0)

        then: "the server fails to unregister but closes normally"
        output == ""
    }

    def "fail to register the server"() {
        given: "the naming server is stopped"
        stopNamingServer()

        when: "a server is started"
        prepareServers(['A'], false)

        and: "the server thread is joined"
        def output = stopServer(0) // This is needed only to get the output, server already failed

        then: "the server fails to register and closes with an error"
        output == ""
    }
}
