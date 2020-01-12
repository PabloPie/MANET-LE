package manet.positioning.strategies;

import java.util.Map;
import java.util.Set;

import manet.communication.Emitter;
import manet.positioning.NextDestinationStrategy;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import manet.positioning.PositioningConfiguration;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.util.ExtendedRandom;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class NextPingPong implements NextDestinationStrategy {

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitter";

	private final int position_pid;
	private final int emitter_pid;


	private boolean first;
	private static Position p1 = new Position(390, 375);
	private static  Position p2 = new Position(703, 370.368);
	private Position p = p1;

	public NextPingPong(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		first = true;
		
	}

	@Override
	public Position getNextDestination(Node host, int speed) {
		final int scope = ((Emitter) host.getProtocol(emitter_pid)).getScope();
		PositionProtocol pos_proto_host = ((PositionProtocol) host.getProtocol(position_pid));
		
		if(host.getID() != 7) return pos_proto_host.getCurrentPosition();
		if(first) {
			first = false;
			return p1;
		} else if(pos_proto_host.getCurrentPosition().equals(p)) {
			
			if(CommonState.getTime()%83 == 0) {
				if(p == p1) p = p2;
				else p = p1;
				return p;
			}
		}
		
		return pos_proto_host.getCurrentDestination();
/*

		if (currentMoving != null) {
			PositionProtocol pos_proto_cur_moving = (PositionProtocol) currentMoving.getProtocol(position_pid);
			if (pos_proto_cur_moving.isMoving()) {// est-il toujours en mouvement
				return pos_proto_host.getCurrentPosition(); // host n'est pas autorisé à bouger.
			} else {
				currentMoving = null;
			}
		}
		
		Position new_position = pos_neigbor.getNewPositionWith(distance, angle).bound(0, 0, pos_proto_host.getMaxX(),
				pos_proto_host.getMaxY());
		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
		positions.put(host.getID(), new_position);
		Map<?, ?> m = PositionProtocol.getConnectedComponents(positions, scope);
		if (m.size() > 1) {
			return pos_proto_host.getCurrentPosition();// le mouvement de host entraine un split du reseau
		}
		currentMoving = host;
		return new_position;*/
	}

}
