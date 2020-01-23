package manet.communication;

import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.Message;

public class EmitterAlt implements Emitter {

	private static final String PAR_LATENCY = "latency";
	private static final String PAR_VARIANCE = "variance";
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_POSITION = "position";

	private final int myPid;
	private final int latency;
	private final boolean variance;
	private final int scope;
	private final int posprotocol;

	public EmitterAlt(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		latency = Configuration.getInt(prefix + "." + PAR_LATENCY);
		variance = Configuration.getBoolean(prefix + "." + PAR_VARIANCE);
		scope = Configuration.getInt(prefix + "." + PAR_SCOPE);
		posprotocol = Configuration.lookupPid(PAR_POSITION);
	}

	public Object clone() {
		manet.communication.EmitterAlt res = null;
		try {
			res = (manet.communication.EmitterAlt) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (event instanceof Message) {
			Message msg = (Message) event;
			if (node.getID() == msg.getIdDest() || msg.getIdDest() == Emitter.ALL)
				EDSimulator.add(0, event, node, msg.getPid());
		}
	}

	@Override
	public void emit(Node host, Message msg) {
		PositionProtocol positionSrc = (PositionProtocol) host.getProtocol(posprotocol);

		for (int destID = 0; destID < Network.size(); destID++) {
			int latency = this.latency;

			PositionProtocol positionDest = (PositionProtocol) Network.get(destID).getProtocol(posprotocol);
			double distance = positionSrc.getCurrentPosition().distance(positionDest.getCurrentPosition());
			if (this.variance)
				latency = CommonState.r.nextPoisson(this.latency);

			if (distance <= this.scope)
				EDSimulator.add(latency, msg, Network.get(destID), myPid);
		}
	}

	@Override
	public int getLatency() {
		return this.latency;
	}

	@Override
	public int getScope() {
		return this.scope;
	}

}
