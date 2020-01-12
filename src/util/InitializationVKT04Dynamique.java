package util;
import manet.positioning.PositionProtocol;
import manet.vkt04.VKT04Dynamique;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.util.ExtendedRandom;

public class InitializationVKT04Dynamique implements Control {
	private static final String PAR_POSITIONPROTOCOL = "position";
	private static final String PAR_NEIGHBORSPROTOCOL = "neighbors";
	private static final String PAR_ELECTIONPROTOCOL = "election";
	private final int pidProtocolPosition;
	private final int pidProtocolNeighbor;
	private final int pidProtocolElection;
	private final int scope;
	
	public static final String loop_event = "LOOPEVENT";
	private static ExtendedRandom myrand = new ExtendedRandom(4757);

	
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
			int r = myrand.nextInt()%20;
			if(r < 0) r = -r;
			vkt.initialiseValueId(new Pair<Integer, Long>(r, node.getID()));

		}
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			
			EDSimulator.add(0, loop_event, node, pidProtocolPosition);
			EDSimulator.add(0, loop_event, node, pidProtocolNeighbor);
			EDSimulator.add(10, "START_ELECTION", node, pidProtocolElection);
		}
		// Pour simuler election avec perte de voisin non parent
		//EDSimulator.add(3295, "START_ELECTION", Network.get(4), pidProtocolElection);
		return false;
	}
}