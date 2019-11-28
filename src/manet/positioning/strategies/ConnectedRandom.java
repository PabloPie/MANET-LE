package manet.positioning.strategies;

import manet.communication.Emitter;
import manet.positioning.InitialPositionStrategy;
import manet.positioning.NextDestinationStrategy;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import manet.positioning.PositioningConfiguration;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.util.ExtendedRandom;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class ConnectedRandom implements InitialPositionStrategy, NextDestinationStrategy {

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitter";

	private final int position_pid;
	private final int emitter_pid;
	private final ExtendedRandom my_random;

	public ConnectedRandom(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		this.my_random = PositioningConfiguration.getPositioningRandom();
	}

	@Override
	public Position getNextDestination(Node host, int speed) {
		Emitter emitter = (Emitter) host.getProtocol(emitter_pid);
		PositionProtocol pos_prot = (PositionProtocol) host.getProtocol(position_pid);
		int scope = emitter.getScope();
		int marge = Math.min(scope, 20);
		double minX = (pos_prot.getMaxX() / 2) - (scope - marge);
		double maxX = (pos_prot.getMaxX() / 2) + (scope - marge);
		double minY = (pos_prot.getMaxY() / 2) - (scope - marge);
		double maxY = (pos_prot.getMaxY() / 2) + (scope - marge);

		return new Position(CommonState.r.nextDouble() * (maxX - minX) + minX,
				my_random.nextDouble() * (maxY - minY) + minY);
	}

	@Override
	public Position getInitialPosition(Node host) {
		return getNextDestination(host, 0);
	}

}
