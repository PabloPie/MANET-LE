package manet.vkt04;

import java.util.List;

import manet.Monitorable;
import manet.algorithm.election.ElectionProtocol;
import manet.detection.NeighborProtocol;
import peersim.config.Configuration;
import peersim.core.Node;



public class VKT04Statique implements Monitorable, ElectionProtocol, NeighborProtocol {
	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_EMITTERPID = "emitterprotocol";
	
	private final int myPid;
	private final int pidPosition;
	private final int pidEmitter;
	
	public VKT04Statique(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pidPosition = Configuration.lookupPid(prefix + "." + PAR_POSITIONPID);
		pidEmitter = Configuration.lookupPid(prefix + "." + PAR_EMITTERPID);
	}
	
	public Object clone() {
		Object res = null;
		try {
			res = (VKT04Statique) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Long> getNeighbors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getIDLeader() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getValue() {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
