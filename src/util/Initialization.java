package util;
import manet.algorithm.election.ElectionProtocol;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class Initialization implements Control {
	private static final String PAR_PROTO = "protocol";
	private final int pidProtocolPosition;
	private final int pidProtocolNeighbor;
	private final int pidProtocolElection;

	public static final String loop_event = "LOOPEVENT";

	
	public Initialization(String prefix) {
		pidProtocolPosition = Configuration.lookupPid("position");
		pidProtocolNeighbor = Configuration.lookupPid("neighbors");
		pidProtocolElection = Configuration.lookupPid("election");
	}

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);

			ElectionProtocol election = (ElectionProtocol) node.getProtocol(pidProtocolElection);
			election.init(node.getID());

			PositionProtocol pos = (PositionProtocol) node.getProtocol(pidProtocolPosition);
			pos.initialiseCurrentPosition(node);

			EDSimulator.add(0, loop_event, node, pidProtocolPosition);
			EDSimulator.add(0, loop_event, node, pidProtocolNeighbor);
		}
		return false;
	}
}
