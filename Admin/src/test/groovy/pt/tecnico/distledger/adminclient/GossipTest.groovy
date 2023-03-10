package pt.tecnico.distledger.adminclient

class GossipTest extends BaseTest {
    // TODO: update tests when gossip is implemented
    def "gossip outputs a todo message"() {
        given: "a gossip input"
        provideInput("gossip\nexit\n")

        when: "the admin client is run"
        runMain()

        then: "the output is correct"
        outBuf.toString() == "> TODO: implement gossip command (only for Phase-3)\n> "
    }
}
