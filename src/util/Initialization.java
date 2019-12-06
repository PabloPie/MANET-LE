package util;
import manet.detection.NeighborProtocol;
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
	
	public static final String loop_event = "LOOPEVENT";

	
	public Initialization(String prefix) {
		pidProtocolPosition = Configuration.lookupPid("position");
		pidProtocolNeighbor = Configuration.lookupPid("neighbors");
	}

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);

			PositionProtocol pos = (PositionProtocol) node.getProtocol(pidProtocolPosition);
			pos.initialiseCurrentPosition(node);
			EDSimulator.add(0, loop_event, node, pidProtocolPosition);
						
			EDSimulator.add(0, loop_event, node, pidProtocolNeighbor);
		}
		return false;
	}
}
