package manet.detection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import manet.communication.EmitterImpl;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.ProbeMessage;

public class NeighborProtocolImpl extends EmitterImpl implements NeighborProtocol {
	
	private static final String PAR_HEARTBEATPERIOD = "heartbeat_period";
	private static final String PAR_NEIGHBOR_TIMER = "neighbor_timer";
	public static final String loop_event = "LOOPEVENT";
	
	private final int neighbor_timer;
	private final int heartbeat_period;
	private Map<Long, Integer> neighbors;
	private int myPid;


	public NeighborProtocolImpl(String prefix) {
		super("protocol.emitter");
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.neighbor_timer = Configuration.getInt(prefix + "." + PAR_NEIGHBOR_TIMER);
		this.heartbeat_period = Configuration.getInt(prefix + "." + PAR_HEARTBEATPERIOD);
		this.neighbors = new ConcurrentHashMap<>();
	}
	
	public Object clone() {
		NeighborProtocolImpl res = (NeighborProtocolImpl) super.clone();
		res.neighbors = new ConcurrentHashMap<>();
		return res;
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {

		if (pid != myPid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if (event instanceof String && event.equals(loop_event)) {
			
			// Heartbeat
			if(CommonState.getTime()%heartbeat_period == 0) {
				emit(node, new ProbeMessage(node.getID(), ALL, pid));
			}
			
			// Mise à jour des timers des voisins
			for(Long id : this.neighbors.keySet()) {
				int timer = this.neighbors.get(id);
				if(timer == 1) this.neighbors.remove(id);
				else this.neighbors.put(id, timer - 1);
			}

			EDSimulator.add(1, loop_event, node, myPid);
		}
		else if(event instanceof ProbeMessage) {
			// On ajoute ou met à jour le voisin et son timer
			this.neighbors.put( ((ProbeMessage)event).getIdSrc(), neighbor_timer );
		}
	}

	@Override
	public final List<Long> getNeighbors() {
		return new ArrayList<Long>(this.neighbors.keySet());
	}
}
