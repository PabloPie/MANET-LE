package manet.vkt04;

import java.util.ArrayList;
import java.util.List;

import manet.Monitorable;
import manet.algorithm.election.ElectionProtocol;
import manet.detection.NeighborProtocol;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.InitializationVKT04Statique.InitializationStaticParameters;


public class VKT04Statique implements Monitorable, ElectionProtocol, NeighborProtocol {
	private static final String PAR_POSITIONPID = "position";
	private static final String PAR_EMITTERPID = "emitter";
	
	public static final String loop_event = "LOOPEVENT";
	
	private final int myPid;
	private final int pidPosition;
	private final int pidEmitter;
	private int myValue;
	private List<Long> myNeighbors;
	
	public VKT04Statique(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pidPosition = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		pidEmitter = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
	}
	
	public Object clone() {
		VKT04Statique res = null;
		try {
			res = (VKT04Statique) super.clone();
			res.myValue = (int)(Math.random() * 100);
			res.myNeighbors = new ArrayList<Long>();
		} catch (CloneNotSupportedException e) {
		}
		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(event instanceof InitializationStaticParameters) {
			InitializationStaticParameters isp = (InitializationStaticParameters)event;
			this.myValue = isp.value;
			this.myNeighbors = isp.neighbors;
			//System.out.println("Node "+node.getID() + " = " + myValue + " -> " + isp.neighbors);
			EDSimulator.add(0, loop_event, node, pid);
		}
		
		
	}

	@Override
	public List<Long> getNeighbors() {
		return this.myNeighbors;
	}

	@Override
	public long getIDLeader() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getValue() {
		return myValue;
	}
}
