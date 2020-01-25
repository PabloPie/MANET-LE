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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
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
    
    // stats
    private IncrementalStats stats;
    private int[] err;
    
    private BufferedWriter bw;

    public ConnexionMonitor(String prefix) throws IOException {
        position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
        election_pid = Configuration.getPid(prefix + "." + PAR_ELECTIONPID);
        scope = Configuration.getInt("protocol.emitter." + PAR_SCOPE);
        stats = new IncrementalStats();
        err = new int[Network.size()];
        
        String algo = Configuration.getString("protocol.election").toLowerCase();
        File file = new File("stats/stats-"+algo+"-"+scope+".csv");
        System.out.println("stats-"+algo+"-"+scope+".csv");
        file.createNewFile();
        bw = new BufferedWriter(new FileWriter(file));
    }

    @Override
    public boolean execute() {
    	long time= CommonState.getTime();
    	
        Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
        Map<Integer, Set<Node>> connexions = PositionProtocol.getConnectedComponents(positions, scope);
        stats.add(connexions.size());
        
        getBadLeaders(connexions);
        
        double instabiliteTotale = Arrays.stream(err).sum() / (time * Network.size() + 0.0);
        try {
	        if(time == 0) {
	        	bw.write("Scope,Average,Variance,Messages sent,Messages received,Instabilite totale\n");
	        }
			else if(CommonState.getTime() == CommonState.getEndTime() - 1) {
				bw.write(scope + "," +
                        stats.getAverage() + "," +
						stats.getVar() + "," +
						EmitterWatcher.msgSent + "," +
						EmitterWatcher.msgReceived + "," +
						instabiliteTotale + "\n");
				bw.flush();
				bw.close();
			}
        }catch(IOException e) {
        	System.out.println(e.getStackTrace());
        	return true;
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
