package manet.communication;

import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import util.Message;

public class EmitterImpl implements Emitter {
	
	private static final String PAR_LATENCY = "latency";
	private static final String PAR_VARIANCE = "variance";
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_POSITION = "position";
	
	public final int myPid;
	public final int latency;
	public final boolean variance;
	public final int scope;
	public final int posprotocol;
	
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
		System.out.println("processEvent dans EmmiterImpl");
		if(event instanceof Message) {
			Message msg = (Message) event;
			PositionProtocol positionSrc = (PositionProtocol) Network.get((int) msg.getIdSrc()).getProtocol(posprotocol);
			PositionProtocol positionDest = (PositionProtocol) node.getProtocol(posprotocol);
			
			if((msg.getIdDest() == Emitter.ALL || msg.getIdDest() == node.getID()) && positionSrc.getCurrentPosition().distance(positionDest.getCurrentPosition()) <= this.scope) {
				EDProtocol p = (EDProtocol) node.getProtocol(msg.getPid());
				p.processEvent(node, msg.getPid(), event);
			}
		}
	}

	@Override
	public void emit(Node host, Message msg) {

		System.out.println("emit dans EmmiterImpl");
		int latency = this.latency;
		for(int i = 0 ; i<Network.size();i++) {
			if(this.variance) latency = CommonState.r.nextPoisson(this.latency);
			EDSimulator.add(latency, msg, Network.get(i), myPid);
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
