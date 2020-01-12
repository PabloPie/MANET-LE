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
import peersim.core.Node;
import util.AckMessage;
import util.ElectionMessage;
import util.LeaderMessage;
import util.Message;
import util.Pair;

public class VKT04Dynamique implements Monitorable, ElectionProtocol, NeighborhoodListener {
	private static final String PAR_EMITTERPID = "emitter";
	private static final String PAR_NEIGHBORSPID = "neighbors";
	
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
	private Set<Long> electionAck;
		
	public VKT04Dynamique(String prefix) {
		String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pidEmitter = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
		pidNeighbors = Configuration.getPid(prefix + "." + PAR_NEIGHBORSPID);
		myState = state.LEADER_UNKNOWN;
		electionStarted = false;
		electionId = nullPair;
		myLeader = nullPair;
	}
	
	public Object clone() {
		VKT04Dynamique res = null;
		try {
			res = (VKT04Dynamique) super.clone();
			res.electionAck = new HashSet<Long>();
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
				this.startNewElection(node);
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
			
			// Initialisation des variables
			this.electionParent = msg.getIdSrc();
			this.myState = state.LEADER_UNKNOWN;
			this.electionStarted = true;
			this.electionAck.clear();
			this.electionId = msg.getElectionId();
			this.electionMax = this.myId;
			this.myState = state.ELECTION;
			
			List<Long> neighbors = this.getNeighbors(node);
			
			// Diffusion à nos enfants qu'une élection est en cours
			if(neighbors.size() > 1) {
				for(long id : neighbors) {
					if(id != this.electionParent) {
						this.emit(node, new ElectionMessage(node.getID(), id, myPid, this.electionId));
					}
				}
			} else { // Si on a pas d'enfant, on envoie un ack à notre parent
				this.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
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
		this.electionAck.add(msg.getIdSrc());
		
		// On garde la plus grande valeur
		this.udpdateMaxNode(msg.getMaxNode());
		
		if(this.electionId._2.equals(node.getID())) { // Si on est l'initiateur de l'élection
			// Si on a reçu un ack de tous nos enfants alors on envoie un message de leader
			if(this.electionAck.size() == neighbors.size()) {
				this.propagateMyLeader(node);
			}
		}
		else { // Si on est pas l'initiateur...
			// Si on a encore un parent...
			if(this.electionParent != -1) {
				// .. et qu'on a reçu un ack de tous nos enfants alors on envoie un ack à notre parent
				if(electionAck.size() == neighbors.size() - 1) {
					this.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
				}
				// Sinon il faut attendre tous les ACKs
			} else { // Si on a plus de parent...
				// .. et qu'on a reçu un ack de tous nos enfants alors on diffuse notre leader
				if(this.electionAck.size() == neighbors.size()) {
					this.propagateMyLeader(node);
				}
				// Sinon il faut attendre tous les ACKs
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
				// TODO
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
		
		// On diffuse l'information sur le leader
		this.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionId, this.electionMax));
		
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
		
		this.myLeader = this.electionMax;
		this.electionStarted = false;
	}
	
	private void startNewElection(Node node) {
		this.electionId = new Pair<Integer, Long>(this.electionId._1 + 1, node.getID());
		this.emit(node, new ElectionMessage(node.getID(), Emitter.ALL, myPid, this.electionId));
		this.electionStarted = true;
		this.electionMax = this.myId;
		this.myState = state.ELECTION;
		this.myLeader = nullPair;
	}
	
	private void propagateMyLeader(Node node) {
		this.emit(node, new LeaderMessage(node.getID(), Emitter.ALL, myPid, this.electionId, this.electionMax));
		this.electionDone(node);
	}

	@Override
	public long getIDLeader() {
		return this.myLeader._2;
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
		res.add("Node " + this.myId + " p=" + this.electionParent);
		res.add("Leader " + this.myLeader);
		res.add("Election " + this.electionId);
		res.add("Max " + this.electionMax);
		res.add("A : " + this.electionAck.toString() + this.electionParent);
		res.add("V : " + this.getNeighbors(host).toString());
		return res;
	}

	@Override
	public void initialiseValueId(Pair<Integer, Long> id) {
		this.myId = id;
	}

	@Override
	public void newNeighborDetected(Node node, long newNeighbor) {
		List<Long> neighbors = this.getNeighbors(node);
		// Si on est en élection
		if(this.electionStarted) {
			// TODO l'élection peut se bloquer car le nouveau voisin ne va pas renvoyer un ACK mais c'est normal vu qu'il ne participe pas à l'léection + surement disctinction si onest init ou pas, parent ou pas
			// En fait il faudrait attendre X ack pour envoyer un ACK au père où X est le nombre de voisins au début de lélection ?
			// On peut jouer avec les paramètres de latency pour voir la diff
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
		
		// TODO : que faire si on a pas de voisin ?
		
		// Si on est en élection
		if(this.electionStarted) {
			// Si on a perdu notre parent
			if(this.electionParent == lostNeighbor) {
				
				this.electionParent = -1;
				// Si tous les ACKs sont reçus alors on transmet un LeaderMessage et on est plus en élection
				if(this.electionAck.size() == neighbors.size()) {
					this.propagateMyLeader(node);
				}
				// Sinon on attend tous les ACKs
				
			} else { // Si on a perdu un voisin non parent
				
				// on le retire des ACKs si présent
				this.electionAck.remove(lostNeighbor);
				// Si tous les ACKs sont reçus alors on transmet un LeaderMessage et on est plus en élection
				if(this.electionAck.size() == neighbors.size()) {
					this.propagateMyLeader(node);
				}
				// Sinon si notre seul voisin est notre parent, on l'ACK
				else if(neighbors.size() == 1 && neighbors.get(0) == this.electionParent) {
					this.emit(node, new AckMessage(node.getID(), this.electionParent, myPid, this.electionId, this.electionMax));
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
	
	private List<Long> getNeighbors(Node node) {
		return ((NeighborProtocol)node.getProtocol(pidNeighbors)).getNeighbors();
	}
	
	private void emit(Node src, Message msg) {
		((Emitter)src.getProtocol(pidEmitter)).emit(src, msg);
	}
}
