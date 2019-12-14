package manet.vkt04;

import java.util.ArrayList;
import java.util.List;

import manet.Monitorable;
import manet.algorithm.election.ElectionProtocol;
import manet.communication.Emitter;
import manet.detection.NeighborProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.AckMessage;
import util.ElectionMessage;
import util.LeaderMessage;
import util.Message;
import util.InitializationVKT04Statique.InitializationStaticParameters;


public class VKT04Statique implements Monitorable, ElectionProtocol, NeighborProtocol {
	private static final String PAR_POSITIONPID = "position";
	private static final String PAR_EMITTERPID = "emitter";
	
	public static final String loop_event = "LOOPEVENT";
	
	private enum state {
		LEADER,
		LEADER_KNWON,
		LEADER_UNKNOWN,
		ELECTION
	}
	
	private final int myPid;
	private final int pidPosition;
	private final int pidEmitter;
	
	private int myValue;
	private List<Long> myNeighbors;
	private state myState;
	private long myLeader;
	
	private boolean electionStarted;
	private boolean electionInitializer;
	private long electionParent;
	private int electionMaxNodeValue;
	private long electionMaxNodeId;
	private int electionAck;
	
	public VKT04Statique(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pidPosition = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		pidEmitter = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		myState = state.LEADER_UNKNOWN;
		electionStarted = false;
		electionInitializer = false;
		myLeader = -1;
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
		if(event instanceof Message) {
			Message msg = (Message)event;
			
			// On ignore les messages que l'on a envoyé
			if(msg.getIdSrc() == node.getID()) return;
			
			if(msg instanceof ElectionMessage)
				this.processElectionMessage(node, (ElectionMessage)msg);
			
			else if(msg instanceof AckMessage)
				this.processAckMessage(node, (AckMessage)msg);
			
			else if(msg instanceof LeaderMessage)
				this.processLeaderMessage(node, (LeaderMessage)msg);
		}
		// Reception de la valeur du neoud et des voisins par la classe init
		else if(event instanceof InitializationStaticParameters) {
			InitializationStaticParameters isp = (InitializationStaticParameters)event;
			this.myValue = isp.value;
			this.myNeighbors = isp.neighbors;
			
			// Démarrage du loo_event
			EDSimulator.add(0, loop_event, node, pid);
		}
		
		else if(event instanceof String) {
			
			// Si on reçoit l'ordre de démarrer une élection
			if(event.equals("START_ELECTION")) {
				Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
				emitter.emit(node, new ElectionMessage(node.getID(), Emitter.ALL, myPid));
				this.electionStarted = true;
				this.electionInitializer = true;
				this.electionMaxNodeId = node.getID();
				this.electionMaxNodeValue = this.myValue;
				this.myState = state.ELECTION;
			}
			else System.out.println("Node " + node.getID() + " : " + event);
		}
		 
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un ElectionMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processElectionMessage(Node node, ElectionMessage msg) {
		Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
		
		// Si on est dans une élection
		if(this.electionStarted) {
			// Si l'émetteur n'est pas notre parent alors on acquitte
			if(msg.getIdSrc() != this.electionParent) {
				emitter.emit(node, new AckMessage(node.getID(), msg.getIdSrc(), myPid, this.electionMaxNodeValue, this.electionMaxNodeId));
			}
			// else TODO : élection concurrente ?
		} else { // C'est une nouvelle élection
			
			// Initialisation des variables
			this.electionParent = msg.getIdSrc();
			this.myState = state.LEADER_UNKNOWN;
			this.electionStarted = true;
			this.electionAck = 0;
			this.electionMaxNodeId = node.getID();
			this.electionMaxNodeValue = this.myValue;
			this.myState = state.ELECTION;
			
			// Diffusion à nos enfants qu'une élection est en cours
			if(this.myNeighbors.size() > 1) {
				for(int i = 0;i<Network.size();i++) {
					long id = Network.get(i).getID();
					if(id != this.electionParent) {
						emitter.emit(node, new ElectionMessage(node.getID(), id, myPid));
					}
				}
			} else { // Si on a pas d'enfant, on envoie un ack à notre parent
				emitter.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionMaxNodeValue, this.electionMaxNodeId));
			}
		}
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un AckMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processAckMessage(Node node, AckMessage msg) {
		Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
		this.electionAck++;
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNodeId(), msg.getMaxValue());
		
		if(this.electionInitializer) { // Si on est l'initiateur de l'élection
			// Si on a reçu un ack de tous nos enfants alors on envoie un message de leader
			if(electionAck == this.myNeighbors.size()) {
				emitter.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionMaxNodeValue, this.electionMaxNodeId));
				this.electionDone(node);
			}
		}
		else { // Si on est pas l'initiateur...
			// .. et qu'on a reçu un ack de tous nos enfants alors on envoie un ack à notre parent
			if(electionAck == this.myNeighbors.size() - 1) {
				emitter.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionMaxNodeValue, this.electionMaxNodeId));
			}
		}
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un LeaderMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processLeaderMessage(Node node, LeaderMessage msg) {
		if(!electionStarted) // TODO : Concurrent election ?
			return;
		
		Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNodeId(), msg.getMaxValue());
		
		// On diffuse l'information sur le leader
		emitter.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionMaxNodeValue, this.electionMaxNodeId));
		
		// On est plus en élection
		this.electionDone(node);
	}
	
	/**
	 * Compare et conserve l'identifiant du noeud ayant la plus grosse valeur
	 * @param nodeid Nouveau noeud à comparer
	 * @param value Valeur du nouveau noeud
	 */
	private void udpdateMaxNode(long nodeid, int value) {
		if(value > this.electionMaxNodeValue) {
			this.electionMaxNodeId = nodeid;
			this.electionMaxNodeValue = value;
		}
		else if(value == this.electionMaxNodeValue && nodeid > this.electionMaxNodeId) {
			this.electionMaxNodeId = nodeid;
		}
	}
	
	/**
	 * Remise à zéro des variables de l'élection et mise à jour du leader
	 * @param n Noeud ayant terminé l'élection
	 */
	private void electionDone(Node n) {
		if(this.electionMaxNodeId == n.getID()) this.myState = state.LEADER;
		else this.myState = state.LEADER_KNWON;
		
		this.myLeader = this.electionMaxNodeId;
		this.electionAck = 0;
		this.electionStarted = false;
		this.electionInitializer = false;
	}

	@Override
	public List<Long> getNeighbors() {
		return this.myNeighbors;
	}

	@Override
	public long getIDLeader() {
		return this.myLeader;
	}

	@Override
	public int getValue() {
		return myValue;
	}
		
	@Override
	public int getState(Node host) {
		switch(this.myState) {
			case LEADER:
				return 6;
			case LEADER_KNWON:
				return 0;
			case LEADER_UNKNOWN:
				return 1;
			case ELECTION:
				return 3;
			default:
				return 7;
		}
	}
	
	@Override
	public List<String> infos(Node host) {
		List<String> res = new ArrayList<String>();
		res.add("Node " + host.getID());
		res.add("Value " + this.myValue);
		res.add("Max " + this.electionMaxNodeValue);
		res.add("Leader " + this.myLeader);
		return res;
	}
}
