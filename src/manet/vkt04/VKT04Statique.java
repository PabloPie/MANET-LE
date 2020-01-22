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
import util.Message;
import util.vkt04.AckMessage;
import util.vkt04.ElectionMessage;
import util.vkt04.LeaderMessage;
import util.vkt04.Pair;

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
	
	private Pair<Integer, Long> myId;
	private List<Long> myNeighbors;
	private state myState;
	private long myLeader;
	
	private boolean electionStarted;
	private Pair<Integer, Long> electionId; // Id élection + Id initiateur pour ordre total
	private long electionParent;
	private Pair<Integer, Long> electionMax;
	private int electionAck;
	
	public VKT04Statique(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pidPosition = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		pidEmitter = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		myState = state.LEADER_UNKNOWN;
		electionStarted = false;
		electionId = new Pair<Integer, Long>(-1,-1L);
		myLeader = -1;
	}
	
	public Object clone() {
		VKT04Statique res = null;
		try {
			res = (VKT04Statique) super.clone();
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
		else if(event instanceof String) {
			
			// Si on reçoit l'ordre de démarrer une élection
			if(event.equals("START_ELECTION")) {
				this.electionId = new Pair<Integer, Long>(this.electionId._1 + 1, node.getID());
				
				Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
				emitter.emit(node, new ElectionMessage(node.getID(), Emitter.ALL, myPid, this.electionId));
				this.electionStarted = true;
				this.electionMax = this.myId;
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
		
		// Si on est déjà dans l'élection et qu'on reçoit un message correspondant à celle-ci
		if(this.electionStarted && msg.getElectionId().equals(this.electionId)) {
			// Si l'émetteur n'est pas notre parent alors on acquitte
			if(msg.getIdSrc() != this.electionParent) {
				emitter.emit(node, new AckMessage(node.getID(), msg.getIdSrc(), myPid, this.electionId, this.electionMax));
			}
		}
		// Sinon si l'ElectiondId du message est suppérieur alors c'est une nouvelle élection
		else if(msg.getElectionId().compareTo(this.electionId) == 1) {
			
			// Initialisation des variables
			this.electionParent = msg.getIdSrc();
			this.myState = state.LEADER_UNKNOWN;
			this.electionStarted = true;
			this.electionAck = 0;
			this.electionId = msg.getElectionId();
			this.electionMax = this.myId;
			this.myState = state.ELECTION;
			
			// Diffusion à nos enfants qu'une élection est en cours
			if(this.myNeighbors.size() > 1) {
				for(int i = 0;i<Network.size();i++) {
					long id = Network.get(i).getID();
					if(id != this.electionParent) {
						emitter.emit(node, new ElectionMessage(node.getID(), id, myPid, this.electionId));
					}
				}
			} else { // Si on a pas d'enfant, on envoie un ack à notre parent
				emitter.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
			}
		}
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un AckMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processAckMessage(Node node, AckMessage msg) {
		// Si ce n'est pas un ACK pour l'élection en cours alors on ignore
		if(!msg.getElectionId().equals(this.electionId))
			return;
		
		Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
		this.electionAck++;
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNode());
		
		if(this.electionId._2.equals(node.getID())) { // Si on est l'initiateur de l'élection
			// Si on a reçu un ack de tous nos enfants alors on envoie un message de leader
			if(electionAck == this.myNeighbors.size()) {
				emitter.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionId, this.electionMax));
				this.electionDone(node);
			}
		}
		else { // Si on est pas l'initiateur...
			// .. et qu'on a reçu un ack de tous nos enfants alors on envoie un ack à notre parent
			if(electionAck == this.myNeighbors.size() - 1) {
				emitter.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
			}
		}
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un LeaderMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processLeaderMessage(Node node, LeaderMessage msg) {
		// Si on est pas en élection ou que le message n'est pas pour l'élection en cours on ingore
		if(!electionStarted || !msg.getElectionId().equals(this.electionId))
			return;
		
		Emitter emitter = (Emitter)node.getProtocol(pidEmitter);
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNode());
		
		// On diffuse l'information sur le leader
		emitter.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionId, this.electionMax));
		
		// On est plus en élection
		this.electionDone(node);
	}
	
	/**
	 * Compare et conserve l'identifiant du noeud ayant la plus grosse valeur
	 * @param nodeid Nouveau noeud à comparer
	 * @param value Valeur du nouveau noeud
	 */
	private void udpdateMaxNode(Pair<Integer, Long> nodeId) {
		if(nodeId.compareTo(this.electionMax) == 1) {
			this.electionMax = nodeId;
		}
	}
	
	/**
	 * Remise à zéro des variables de l'élection et mise à jour du leader
	 * @param n Noeud ayant terminé l'élection
	 */
	private void electionDone(Node n) {
		if(this.electionMax._2 == n.getID()) this.myState = state.LEADER;
		else this.myState = state.LEADER_KNWON;
		
		this.myLeader = this.electionMax._2;
		this.electionAck = 0;
		this.electionStarted = false;
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
		return myId._1;
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
		res.add("Node " + this.myId._2 + " (" + this.myId._1 + ")");
		res.add("Leader " + this.myLeader);
		res.add("Election " + this.electionId);
		return res;
	}

	@Override
	public void initialiseNeighbors(List<Long> neighbors) {
		this.myNeighbors = neighbors;
	}

	@Override
	public void init(long id) {
		this.myId = new Pair<>((int)Math.random()%10, id);
	}
}
