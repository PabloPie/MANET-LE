package manet.positioning.strategies;

import manet.positioning.NextDestinationStrategy;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.Node;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public class NextDestinationImmobility implements NextDestinationStrategy {

	private static final String PAR_POSITIONPID = "positionprotocol";
	private final int position_pid;

	public NextDestinationImmobility(String prefix) {
		position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
	}

	@Override
	public Position getNextDestination(Node host, int speed) {

		return ((PositionProtocol) host.getProtocol(position_pid)).getCurrentPosition();
	}

}
