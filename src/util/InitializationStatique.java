package util;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import manet.communication.Emitter;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import manet.vkt04.VKT04Statique;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class InitializationStatique implements Control {
	private static final String PAR_POSITIONPROTOCOL = "position";
	private static final String PAR_NEIGHBORSPROTOCOL = "neighbors";
	private final int pidProtocolPosition;
	private final int pidProtocolNeighbor;
	private final int scope;
	
	public static final String loop_event = "LOOPEVENT";

	
	public InitializationStatique(String prefix) {
		pidProtocolPosition = Configuration.lookupPid(PAR_POSITIONPROTOCOL);
		pidProtocolNeighbor = Configuration.lookupPid(PAR_NEIGHBORSPROTOCOL);
		scope = Configuration.getInt("protocol.emitter.scope");
	}

	@Override
	public boolean execute() {
		// La valeur de la Map est un tuple de l'ID du noeud avec sa position
		Map<Integer, Map.Entry<Long, Position> > nodesIdAndPosition = new HashMap<>();
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			PositionProtocol pos = (PositionProtocol) node.getProtocol(pidProtocolPosition);
			pos.initialiseCurrentPosition(node);
			
			nodesIdAndPosition.put(i, 
					new AbstractMap.SimpleEntry<>(node.getID(), pos.getCurrentPosition()));
		}
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			VKT04Statique vkt = (VKT04Statique) node.getProtocol(pidProtocolNeighbor);
			
			List<Long> neighbors = new ArrayList<>();
			for (int j = 0; j < Network.size(); j++) {
				if(i == j) continue;
				
				// Si la position du noeud i est à distance inferieure à scope de la position du noeud j alors c'est un voisin
				if(nodesIdAndPosition.get(i).getValue().distance(nodesIdAndPosition.get(j).getValue()) <= scope) {
					neighbors.add(nodesIdAndPosition.get(j).getKey());
				}
			}
			
			vkt.init(node.getID());
			vkt.initialiseNeighbors(neighbors);

			EDSimulator.add(0, loop_event, node, pidProtocolPosition);
		}

		EDSimulator.add(1500, "START_ELECTION", Network.get(2), pidProtocolNeighbor);
		
		return false;
	}
}