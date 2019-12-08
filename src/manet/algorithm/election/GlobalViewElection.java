package manet.algorithm.election;

import manet.Monitorable;
import manet.communication.Emitter;
import manet.communication.EmitterImpl;
import manet.detection.NeighborProtocol;
import manet.detection.NeighborhoodListener;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.ExtendedRandom;

import peersim.config.Configuration;

import java.util.*;

public class GlobalViewElection implements ElectionProtocol, Monitorable, NeighborhoodListener {

    // id of a node and its value
    public static class Peer {
        public long id;
        public int value;
    }

    public static class View {
        public int clock;
        public Set<Peer> neighbors = new HashSet<>();
    }

    // Protocol configuration variables
    private static final String PAR_NEIGHBORPID = "neighborprotocol";
    private static final String PAR_EMITTERPID = "emitterprotocol";
    private static final String PAR_SEED = "seed_value";
    private final ExtendedRandom valueRandom;
    private final int my_pid;
    private int emit_pid;
    private int neighbor_pid;

    // Election variables
    private long leader;
    private int value;
    private View[] knowledge = new View[Network.size()]; // XXX: maybe not a good idea for a large network

    /*
     *  INITIALIZATION BLOCK
     */
    public GlobalViewElection(String prefix) {
        String tmp[] = prefix.split("\\.");
        my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
        this.valueRandom = new ExtendedRandom(Configuration.getInt(prefix + "." + PAR_SEED));
        emit_pid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
        neighbor_pid = Configuration.getPid(prefix + "." + PAR_NEIGHBORPID);
        value = valueRandom.nextInt();
    }

    public Object clone() {
        GlobalViewElection election = null;
        try {
            election = (GlobalViewElection) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        election.value = valueRandom.nextInt();
        election.knowledge = new View[Network.size()];
        return election;
    }

    public void initSelfKnowledge(long i) {
        Peer p = new Peer();
        p.id = i;
        p.value = this.value;
        // XXX: should we init every other knowledge entry to -1 ?
        knowledge[(int)i].clock = 0;
        knowledge[(int)i].neighbors.add(p);
    }

    /*
     *  EVENTS BLOCK
     */
    public void lostNeighborDetected(Node host, long id_lost_neighbor) {

    }

    public void newNeighborDetected(Node host, long id_new_neighbor) {
        knowledge[(int)id_new_neighbor].clock++;
        Peer p = new Peer();
        ElectionProtocol gv = (ElectionProtocol) Network.get((int)id_new_neighbor).getProtocol(my_pid);
        p.id = id_new_neighbor;
        p.value = gv.getValue();
        knowledge[(int)id_new_neighbor].neighbors.add(p);
        Emitter e = (EmitterImpl) host.getProtocol(emit_pid);
        // need a message to send knowledge
        // e.emit(host, knowledge);
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {

    }

    /*
     * GETTERS
     */
    @Override
    public long getIDLeader() {
        return this.leader;
    }

    @Override
    public int getValue() {
        return this.value;
    }
}
