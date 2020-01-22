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


	private boolean first7;
	private boolean first8;
	private static Position p1 = new Position(390, 375);
	private static  Position p2 = new Position(719, 370.368);
	private static Position p3, p4, pp;
	private Position p = p1;

	public NextPingPong(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		first7 = true;
		first8 = true;
	}

	@Override
	public Position getNextDestination(Node host, int speed) {
		final int scope = ((Emitter) host.getProtocol(emitter_pid)).getScope();
		PositionProtocol pos_proto_host = ((PositionProtocol) host.getProtocol(position_pid));
		
		if(host.getID() == 7) {
			if(first7) {
				first7 = false;
				return p1;
			} else if(pos_proto_host.getCurrentPosition().equals(p)) {
				
				if(CommonState.getTime()%803 == 0) {
					if(p == p1) p = p2;
					else p = p1;
					return p;
				}
			}
			return pos_proto_host.getCurrentDestination();
		}  else if(host.getID() == 8) {
			if(first8) {
				first8 = false;
				p3 = pos_proto_host.getCurrentPosition();
				p4 = new Position(p3.getX() - 100, p3.getY() + 100);
				pp = p4;
				return pp;
			}else if(pos_proto_host.getCurrentPosition().equals(pp)) {
				
				if(CommonState.getTime()%1703 == 0) {
					if(pp == p3) pp = p4;
					else pp = p3;
					return pp;
				}
			}
		}
		
		return pos_proto_host.getCurrentPosition();
	}

}
