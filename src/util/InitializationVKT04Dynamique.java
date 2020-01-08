package util;
import manet.positioning.PositionProtocol;
import manet.vkt04.VKT04Dynamique;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class InitializationVKT04Dynamique implements Control {
	private static final String PAR_POSITIONPROTOCOL = "position";
	private static final String PAR_NEIGHBORSPROTOCOL = "neighbors";
	private static final String PAR_ELECTIONPROTOCOL = "election";
	private final int pidProtocolPosition;
	private final int pidProtocolNeighbor;
	private final int pidProtocolElection;
	private final int scope;
	
	public static final String loop_event = "LOOPEVENT";

	
	public InitializationVKT04Dynamique(String prefix) {
		pidProtocolPosition = Configuration.lookupPid(PAR_POSITIONPROTOCOL);
		pidProtocolNeighbor = Configuration.lookupPid(PAR_NEIGHBORSPROTOCOL);
		pidProtocolElection = Configuration.lookupPid(PAR_ELECTIONPROTOCOL);
		scope = Configuration.getInt("protocol.emitter.scope");
	}

	@Override
	public boolean execute() {		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			PositionProtocol pos = (PositionProtocol) node.getProtocol(pidProtocolPosition);
			pos.initialiseCurrentPosition(node);
			
			VKT04Dynamique vkt = (VKT04Dynamique) node.getProtocol(pidProtocolElection);			
			vkt.initialiseValueId(new Pair<Integer, Long>((int)(Math.random()*10), node.getID()));

		}
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			
			EDSimulator.add(0, loop_event, node, pidProtocolPosition);
			EDSimulator.add(0, loop_event, node, pidProtocolNeighbor);
			EDSimulator.add(7500 + node.getID(), "START_ELECTION", node, pidProtocolElection);
		}
		return false;
	}
}