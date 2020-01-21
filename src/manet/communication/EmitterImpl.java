package manet.communication;

import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.Message;

public class EmitterImpl implements Emitter {
	
	private static final String PAR_LATENCY = "latency";
	private static final String PAR_VARIANCE = "variance";
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_POSITION = "position";
	
	private final int myPid;
	private final int latency;
	private final boolean variance;
	private final int scope;
	private final int posprotocol;
	
	public EmitterImpl(String prefix)
	{
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		latency = Configuration.getInt(prefix + "." + PAR_LATENCY);
		variance = Configuration.getBoolean(prefix + "." + PAR_VARIANCE);
		scope = Configuration.getInt(prefix + "." + PAR_SCOPE);
		posprotocol = Configuration.lookupPid(PAR_POSITION);
	}
	
	public Object clone() {
		EmitterImpl res = null;
		try {
			res = (EmitterImpl) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(event instanceof Message) {
			Message msg = (Message) event;
			PositionProtocol positionSrc = (PositionProtocol) Network.get((int) msg.getIdSrc()).getProtocol(posprotocol);
			PositionProtocol positionDest = (PositionProtocol) node.getProtocol(posprotocol);
			
			if((msg.getIdDest() == Emitter.ALL || msg.getIdDest() == node.getID())
					&& positionSrc.getCurrentPosition().distance(positionDest.getCurrentPosition()) <= this.scope)
				EDSimulator.add(0, event, node, msg.getPid());
		}
	}

	@Override
	public void emit(Node node, Message msg) {
		int latency = this.latency;
		PositionProtocol positionSrc = (PositionProtocol) node.getProtocol(posprotocol);
		if(msg.getIdDest() == Emitter.ALL) {
			for(int i = 0 ; i<Network.size();i++) {
				
				if(i == node.getIndex()) continue;
				
				PositionProtocol positionDest = (PositionProtocol) Network.get(i).getProtocol(posprotocol);
				if(positionSrc.getCurrentPosition().distance(positionDest.getCurrentPosition()) <= this.scope) {
					if(this.variance) latency = CommonState.r.nextPoisson(this.latency);
					EDSimulator.add(latency, msg, Network.get(i), myPid);
				}
			}
		} else {
			PositionProtocol positionDest = (PositionProtocol) Network.get((int)msg.getIdDest()).getProtocol(posprotocol);
			if(positionSrc.getCurrentPosition().distance(positionDest.getCurrentPosition()) <= this.scope) {
				if(this.variance) latency = CommonState.r.nextPoisson(this.latency);
				EDSimulator.add(latency, msg, Network.get((int)msg.getIdDest()), myPid);
			}
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
