package manet.positioning.strategies;

import java.util.HashMap;
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
public class NextPingPongBlock implements NextDestinationStrategy {

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitter";

	private final int position_pid;
	private final int emitter_pid;
	
	private HashMap<Long, Position> posInit = new HashMap<Long, Position>();
	private HashMap<Long, Position> posDest = new HashMap<Long, Position>();
	private HashMap<Long, Boolean> posRetour = new HashMap<Long, Boolean>();

	public NextPingPongBlock(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
	}

	@Override
	public Position getNextDestination(Node host, int speed) {

		final int scope = ((Emitter) host.getProtocol(emitter_pid)).getScope();
		PositionProtocol pos_proto_host = ((PositionProtocol) host.getProtocol(position_pid));
		if(host.getID()%2 == 0) return pos_proto_host.getCurrentPosition();

		if(!this.posInit.containsKey(host.getID())) {
			this.posInit.put(host.getID(), pos_proto_host.getCurrentPosition());
			this.posRetour.put(host.getID(), false);
			this.posDest.put(host.getID(),new Position(pos_proto_host.getCurrentPosition().getX() - 340, pos_proto_host.getCurrentPosition().getY()));
		}

		if(pos_proto_host.getCurrentPosition() == posDest.get(host.getID())) {
			if(CommonState.getTime()%1513 == 0)this.posRetour.put(host.getID(), true);
		} else if(pos_proto_host.getCurrentPosition() == posInit.get(host.getID())) {
			this.posRetour.put(host.getID(), false);
		}

		return posRetour.get(host.getID())?this.posInit.get(host.getID()):this.posDest.get(host.getID());
	}

}
