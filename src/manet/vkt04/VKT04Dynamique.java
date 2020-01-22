package manet.vkt04;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import manet.Monitorable;
import manet.algorithm.election.ElectionProtocol;
import manet.communication.Emitter;
import manet.detection.NeighborProtocol;
import manet.detection.NeighborhoodListener;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.Message;
import util.vkt04.AckMessage;
import util.vkt04.BeaconMessage;
import util.vkt04.ElectionMessage;
import util.vkt04.LeaderMessage;
import util.vkt04.Pair;

public class VKT04Dynamique implements Monitorable, ElectionProtocol, NeighborhoodListener {
	private static final String PAR_EMITTERPID = "emitter";
	private static final String PAR_NEIGHBORSPID = "neighbors";
	private static final String PAR_BEACON_INTERVAL = "beacon_interval";
	private static final String PAR_BEACON_LOSS = "beacon_max_loss";
	
	public static final String loop_event = "LOOPEVENT";
	
	public static final Pair<Integer, Long> nullPair = new Pair<>(-1,-1L); 
	
	private enum state {
		LEADER,
		LEADER_KNWON,
		LEADER_UNKNOWN,
		ELECTION
	}
	
	private final int myPid;
	private final int pidEmitter;
	private final int pidNeighbors;

	
	private Pair<Integer, Long> myId;
	private state myState;
	private Pair<Integer, Long> myLeader;
	
	private boolean electionStarted;
	private Pair<Integer, Long> electionId; // Id élection + Id initiateur pour ordre total
	private long electionParent;
	private Pair<Integer, Long> electionMax;
	private Set<Long> electionWaitingAcks;
	
	private Set<Long> electionMergeRequests;
	private Pair<Integer, Long> electionMergeMax;
	
	private int beaconTimer;
	private int beaconLoss;
	private long beaconLastTimestamp;
	private final int beaconInterval;
	private final int beaconMaxLoss;

	public VKT04Dynamique(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pidEmitter = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		pidNeighbors = Configuration.getPid(prefix + "." + PAR_NEIGHBORSPID);
		myState = state.LEADER_UNKNOWN;
		electionStarted = false;
		electionId = nullPair;
		myLeader = nullPair;
		electionParent = -1;
		electionMergeMax = nullPair;
		beaconInterval = Configuration.getInt(prefix + "." + PAR_BEACON_INTERVAL);
		beaconMaxLoss = Configuration.getInt(prefix + "." + PAR_BEACON_LOSS);
		beaconTimer = -1;
		beaconLastTimestamp = -1;
		beaconLoss = -1;
	}
	
	public Object clone() {
		VKT04Dynamique res = null;
		try {
			res = (VKT04Dynamique) super.clone();
			res.electionWaitingAcks = new HashSet<Long>();
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
			
			else if(msg instanceof BeaconMessage)
				this.processBeaconMessage(node, (BeaconMessage)msg);
		}
		else if(event instanceof String) {
			
			// Si on reçoit l'ordre de démarrer une élection
			if(event.equals("START_ELECTION")) {
				this.startNewElection(node);
			}
			else if(event.equals("LOOP_BEACON")) {
				this.processBeaconLoop(node);
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
		
		// Si on est déjà dans l'élection et qu'on reçoit un message correspondant à celle-ci
		if(this.electionStarted && msg.getElectionId().equals(this.electionId)) {
			// Si l'émetteur n'est pas notre parent alors on acquitte
			if(msg.getIdSrc() != this.electionParent) {
				this.emit(node, new AckMessage(node.getID(), msg.getIdSrc(), myPid, this.electionId, this.electionMax));
			}
		}
		// Sinon si l'ElectiondId du message est suppérieur alors c'est une nouvelle élection
		else if(msg.getElectionId().compareTo(this.electionId) == 1) {
			List<Long> neighbors = this.getNeighbors(node);
			
			// Initialisation des variables
			this.electionParent = msg.getIdSrc();
			this.myState = state.LEADER_UNKNOWN;
			this.electionStarted = true;
			
			this.electionMergeRequests = new HashSet<>();
			this.electionMergeMax = nullPair;
			this.electionWaitingAcks = new HashSet<>(neighbors);
			this.electionWaitingAcks.remove(this.electionParent);
			
			this.electionId = msg.getElectionId();
			this.electionMax = this.myId;
			this.myState = state.ELECTION;
			
			this.resetBeaconTimer();
			
			// Si on a pas d'enfant, on ACK directement le parent
			if(electionWaitingAcks.isEmpty()) {
				this.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
			}
			else { // Sinon si on a des enfants on leur indique qu'il y a une nouvelle élection
				for(long id : electionWaitingAcks) {
					this.emit(node, new ElectionMessage(node.getID(), id, myPid, this.electionId));
				}
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
		
		List<Long> neighbors = this.getNeighbors(node);
		
		// On ajoute l'emetteur dans notre liste d'ACK
		this.electionWaitingAcks.remove(msg.getIdSrc());
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNode());
		
		if(this.electionWaitingAcks.isEmpty()) {
			// Si on est l'initiateur ou que l'on a plus de parent
			if(this.electionParent == -1) {
				this.propagateMyLeader(node);
			} else {
				this.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
			}	
		}
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un LeaderMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processLeaderMessage(Node node, LeaderMessage msg) {
		List<Long> neighbors = this.getNeighbors(node);
		
		// Si c'est un merge
		if(msg.getElectionId() == null) {
			// Si on est dans une élection alors il fut envoyer le leader quand l'léeciton est terminée
			if(this.electionStarted) {
				if(msg.getMaxNode().compareTo(this.electionMergeMax) == 1) {
					this.electionMergeMax = msg.getMaxNode();
				}
			} else { // Je ne suis pas dans une élection
				// Si le leader reçu est plus grand, il devient mon leader et je transmets à mes autres voisins
				if(msg.getMaxNode().compareTo(this.electionMax) == 1) {
					this.electionMax = msg.getMaxNode();
					this.electionDone(node);
					for(long id : neighbors) {
						if(id != msg.getIdSrc()) {
							this.emit(node, new LeaderMessage(node.getID(), id, myPid, null, this.electionMax));
						}
					}
				} 
				// Sinon on ne fait rien
			}
			return;
		}
		
		// Si on est plus en élection mais qu'on reçoit un leader plus grand pour la même élection
		if(!electionStarted && msg.getElectionId().equals(this.electionId) && msg.getMaxNode().compareTo(this.electionMax) == 1) {
			this.electionMax = msg.getMaxNode();
			this.propagateMyLeader(node);
			return;
		}
		
		// Si on est pas en élection ou que le message n'est pas pour l'élection en cours on ingore
		if(!electionStarted || !msg.getElectionId().equals(this.electionId))
			return;
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNode());
		
		// TODO : traiter les merges en fin d'élection
		
		if(!this.electionMergeRequests.isEmpty()) {
			if(this.electionMergeMax.compareTo(this.electionMax) == 1) {
				this.electionMax = this.electionMergeMax;
			}
			for(long id : this.electionMergeRequests) {
				if(neighbors.contains(id)) {
					this.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, null, this.electionMax));
				}
			}
			this.electionMergeRequests.clear();
		}
		
		// On diffuse l'information sur le leader
		this.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionId, this.electionMax));
		
		// On est plus en élection
		this.electionDone(node);
	}
	
	/**
	 * Traitement à réaliser lors de la réception d'un BeaconMessage
	 * @param node Noeud ayant reçu le message
	 * @param msg Message reçu
	 */
	private void processBeaconMessage(Node node, BeaconMessage msg) {
		// Si on a reçu un Beacon concernant notre leader plus récent que le dernier reçu
		if(msg.getLeader().equals(this.myLeader) && msg.getTimestamp() > this.beaconLastTimestamp) {
			// On met à jour nos compteurs
			this.beaconLastTimestamp = msg.getTimestamp();
			this.beaconLoss = 0;
			this.beaconTimer = this.beaconInterval;
			
			// On diffuse à tous nos voisins sauf l'emetteur le Beacon
			for(long id : this.getNeighbors(node)) {
				if(id != msg.getIdSrc()) {
					this.emit(node, new BeaconMessage(node.getID(), id, myPid, this.myLeader, this.beaconLastTimestamp));
				}
			}
		}
	}
	
	/**
	 * Traitement à réaliser pour la mise à jour du compteur de beacons 
	 * @param node Noeud
	 */
	private void processBeaconLoop(Node node) {
		
		// Si on est le leader on transmet un beacon si on a des voisins
		if(this.myLeader.equals(this.myId)) {
			if(!this.getNeighbors(node).isEmpty())
				this.emit(node, new BeaconMessage(node.getID(), Emitter.ALL, myPid, this.myLeader, this.beaconLastTimestamp++));
		}
		else { // Si on est pas le leader
			// Si le timer tombe à 0 on incrémente le compteur des beacons perdus
			if(this.beaconTimer-- == 0) {
				this.beaconTimer = this.beaconInterval;
				// Si on a perdu trop de beacons on lance une élection
				if(this.beaconLoss++ == this.beaconMaxLoss) {
					this.startNewElection(node);
				}
			}
		}
		EDSimulator.add(1, "LOOP_BEACON", node, myPid);
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
	private void electionDone(Node node) {	
		
		if(this.electionMax._2 == node.getID()) this.myState = state.LEADER;
		else this.myState = state.LEADER_KNWON;
		
		this.myLeader = this.electionMax;
		this.electionStarted = false;
		
		this.beaconLastTimestamp = -1;
		this.beaconTimer = this.beaconInterval;
		this.beaconLoss = 0;
		
		if(this.myLeader.equals(this.myId) && !this.getNeighbors(node).isEmpty()) {
			this.emit(node, new BeaconMessage(node.getID(), Emitter.ALL, myPid, this.myLeader, this.beaconLastTimestamp++));
		} 
	}
	
	/**
	 * Commencer une nouvelle élection 
	 * @param node Noeud démarrant son élection
	 */
	private void startNewElection(Node node) {
		List<Long> neighbors = this.getNeighbors(node);
		
		this.electionParent = -1;
		this.electionId = new Pair<Integer, Long>(this.electionId._1 + 1, node.getID());
		this.electionMax = this.myId;
		
		this.resetBeaconTimer();
		
		// Si pas de voisin alors on est leader
		if(neighbors.isEmpty()) {
			this.myState = state.LEADER;
			this.myLeader = this.myId;
			this.electionStarted = false;
		} else {
			this.electionStarted = true;
			this.electionWaitingAcks = new HashSet<>(neighbors);
			this.electionMergeRequests = new HashSet<>();
			this.electionMergeMax = nullPair;
			this.myState = state.ELECTION;
			this.myLeader = nullPair;
			this.emit(node, new ElectionMessage(node.getID(), Emitter.ALL, myPid, this.electionId));
		}
	}
	
	/**
	 * Remise à zéro des variables du compteur de beacons
	 */
	private void resetBeaconTimer() {
		this.beaconTimer = this.beaconInterval;
		this.beaconLoss = 0;
		this.beaconLastTimestamp = -1;
	}
	
	/**
	 * Diffuse un MessageLeader aux voisins
	 * @param node Noeud propageant son leader
	 */
	private void propagateMyLeader(Node node) {
		if(this.electionMergeMax.compareTo(this.electionMax) == 1) {
			this.electionMax = this.electionMergeMax;
		}
		
		this.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionId, this.electionMax));
		this.electionDone(node);
	}

	/*** ElectionProtocol ***/
	
	@Override
	public long getIDLeader() {
		return this.myLeader._2;
	}

	@Override
	public int getValue() {
		return myId._1;
	}

	@Override
	public void init(long nodeId) {
		this.myId = new Pair<Integer, Long>( (int) nodeId, nodeId);
		Node node = Network.get((int)nodeId);
		EDSimulator.add(1, "LOOP_BEACON", node, myPid);
		EDSimulator.add(1, "START_ELECTION", node, myPid);
	}
	
	/*** Monitorable ***/
	
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
		res.add("N " + this.myId);
		res.add("L " + this.myLeader);
//		res.add("TI " + this.beaconTimer + " TS " + this.beaconLastTimestamp + " LS " + this.beaconLoss);
		res.add("E " + this.electionId);
//		res.add("Max " + this.electionMax);
//		res.add("A : " + this.electionWaitingAcks.toString());
//		res.add("V : " + this.getNeighbors(host).toString());
//		res.add("R : " + this.electionMergeRequests + " " + this.electionMergeMax);
		return res;
	}

	@Override
	public int nbState() {
		return 4; // Je n'ai pas de leader, Je suis un leader, J'ai un leader, Je suis en élection
	}

	/*** NeighborhoodListener ***/

	@Override
	public void newNeighborDetected(Node node, long newNeighbor) {
		List<Long> neighbors = this.getNeighbors(node);
		// Si on est en élection
		if(this.electionStarted) {
			// Il faut notifier le nouveau noeud de notre leader quand l'élection est terminée
			this.electionMergeRequests.add(newNeighbor);
		} else { // Si c'est pas une élection
			// Si on a un leader
			if(!this.myLeader.equals(nullPair)) {
				this.emit(node, new LeaderMessage(node.getID(), newNeighbor, myPid, null, this.electionMax));
			} else { // Si on a pas de leader
				// TODO on a pas de leader ?! on lance une élection ?
			}
		}
	}

	@Override
	public void lostNeighborDetected(Node node, long lostNeighbor) {
		List<Long> neighbors = this.getNeighbors(node);
				
		// Si on est en élection
		if(this.electionStarted) {
			// Si on a perdu notre parent
			if(this.electionParent == lostNeighbor) {
				
				this.electionParent = -1;
				if(this.electionWaitingAcks.isEmpty()) {
					this.propagateMyLeader(node);
				}
				
			} else { // Si on a perdu un voisin non parent
				
				// on le retire des ACKs en attente
				this.electionWaitingAcks.remove(lostNeighbor);
				// on le retire des MergeRequests en attente
				this.electionMergeRequests.remove(lostNeighbor);
				
				// Si on a reçu tous nos ACKs
				if(this.electionWaitingAcks.isEmpty()) {
					// Si on est initiateur ou qu'on a plus de parent on propage notre leader
					if(this.electionParent == -1) {
						this.propagateMyLeader(node);
					} else { // Sinon on ACK notre voisin
						this.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
					}
					
				}
				// Sinon on attend tous les ACKs
				
			}
		} else { // Si c'est pas une élection
			// Si on a pas de leader
			if(this.myLeader.equals(nullPair)) {
				// TODO ca devrait pas arriver ?
			}
			// Si on a perdu notre leader
			else if(this.myLeader._2 == lostNeighbor) {
				// TODO
			} else { // Si on a perdu un voisin non leader
				// TODO on n'a rien à faire ?
			}
		}
	}
	
	/*** Fonctions utiles ***/
	
	/**
	 * Obtenir la liste des voisins en utilisant le NeighborsProtocol
	 * @return Liste des voisins du noeud
	 */
	private List<Long> getNeighbors(Node node) {
		return ((NeighborProtocol)node.getProtocol(pidNeighbors)).getNeighbors();
	}
	
	/**
	 * Diffuser un message en utilisant l'émitteur passez en configuration
	 * @param src Noeud émetteur
	 * @param msg Message à envoyer
	 */
	private void emit(Node src, Message msg) {
		((Emitter)src.getProtocol(pidEmitter)).emit(src, msg);
	}
}
