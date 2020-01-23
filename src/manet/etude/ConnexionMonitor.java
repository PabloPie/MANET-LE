package manet.etude;

import manet.algorithm.election.ElectionProtocol;
import manet.communication.EmitterWatcher;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class ConnexionMonitor implements Control{

    private static final String PAR_POSITIONPID = "positionprotocol";
    private static final String PAR_ELECTIONPID = "electionprotocol";
    private static final String PAR_SCOPE = "scope";

    private final int scope;
    private final int position_pid;
    private final int election_pid;
    
    private static final Logger LOGGER = Logger.getLogger(ConnexionMonitor.class.getName());
    FileHandler fh;
    static{ System.setProperty("java.util.logging.SimpleFormatter.format",
                "%5$s %n");}

    // stats
    private IncrementalStats stats;
    private int[] err;

    public ConnexionMonitor(String prefix) {
        position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
        election_pid = Configuration.getPid(prefix + "." + PAR_ELECTIONPID);
        scope = Configuration.getInt("protocol.emitter." + PAR_SCOPE);
        stats = new IncrementalStats();
        err = new int[Network.size()];
        try {
			fh = new FileHandler("stats.csv");
		} catch (Exception e) {
			e.printStackTrace();
		}
//        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(fh);
    }

    @Override
    public boolean execute() {
    	long time= CommonState.getTime();
    	
        Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
        Map<Integer, Set<Node>> connexions = PositionProtocol.getConnectedComponents(positions, scope);
        stats.add(connexions.size());
        int badLeaders = getBadLeaders(connexions);
        
        double instabiliteTotale = Arrays.stream(err).sum() / (time * Network.size() + 0.0);
        if(time == 0) {
        	LOGGER.info("Connected components,Average,Variance,Messages sent,Messages received,Instabilite totale");
        }
        if(time%1000 == 0) {
        	LOGGER.info(connexions.size() + "," +
        			stats.getAverage() + "," +
        			EmitterWatcher.msgSent+ "," +
        			EmitterWatcher.msgReceived+ "," +
        			instabiliteTotale);
        }
        return false;
    }

	private int getBadLeaders(Map<Integer, Set<Node>> components) {
		int incorrect = 0;
		for(Set<Node> component: components.values()) {
			long leader = Collections.max(component, getNodeComparator()).getID();
			for(Node n: component) {
				int nodeid = (int)n.getID();
				ElectionProtocol election = (ElectionProtocol) n.getProtocol(election_pid);
				if(election.getIDLeader() != leader) {
					err[nodeid]++;
					incorrect++;
				}
			}
		}
		return incorrect;
	}
	
	private static Comparator<Node> getNodeComparator() {
		return (Node o1, Node o2)->((Long)o1.getID()).compareTo(o2.getID());
	}

			
    
    
}
