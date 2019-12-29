package manet.algorithm.election;

import manet.Monitorable;
import manet.communication.Emitter;
import manet.detection.NeighborhoodListener;
import peersim.core.Network;
import peersim.core.Node;

import peersim.config.Configuration;
import util.Message;
import util.globalview.Edit;
import util.globalview.EditMessage;
import util.globalview.KnowledgeMessage;
import util.globalview.View;

import java.util.*;

public class GlobalViewElection implements ElectionProtocol, Monitorable, NeighborhoodListener, ElectionInit {

    // Protocol configuration variables
    private static final String PAR_NEIGHBORPID = "neighborprotocol";
    private static final String PAR_EMITTERPID = "emitterprotocol";
    private static final String PAR_SEED = "seed_value";
    private final int myPid;
    private int emitPid;
    private int neighborPid;

    // Aux
    private int myid;

    // Election variables
    private long leader;
    private int leaderval = -1;
    private int value;
    private View[] knowledge = new View[Network.size()];

    /*
     *  INITIALIZATION BLOCK
     */
    public GlobalViewElection(String prefix) {
        String tmp[] = prefix.split("\\.");
        myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
        emitPid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
        neighborPid = Configuration.getPid(prefix + "." + PAR_NEIGHBORPID);
    }

    public Object clone() {
        GlobalViewElection election = null;
        try {
            election = (GlobalViewElection) super.clone();
            election.knowledge = new View[Network.size()];
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return election;
    }

    public void initializeValues(long id) {
        myid = (int) id;
        this.value = myid;
        knowledge[myid] = new View(0);
        knowledge[myid].addNeighbor(id, this.value);
        calculateLeader();
    }

    /*
     *  EVENTS BLOCK
     */
    public void lostNeighborDetected(Node host, long id_lost_neighbor) {
        Edit edit = new Edit(host.getID(), knowledge[myid].clock, knowledge[myid].clock+1);
        edit.addRemoved(id_lost_neighbor, knowledge[myid].getValue(id_lost_neighbor));
        knowledge[myid].removeNeighbor(id_lost_neighbor);
        knowledge[myid].clock++;
        calculateLeader();
        Emitter e = (Emitter) host.getProtocol(emitPid);
        EditMessage editMsg = new EditMessage(myid, Emitter.ALL, myPid, edit);
        e.emit(host, editMsg);
    }

    public void newNeighborDetected(Node host, long id_new_neighbor) {
        // is this actually allowed?
        ElectionProtocol gv = (ElectionProtocol) Network.get((int)id_new_neighbor).getProtocol(myPid);
        knowledge[myid].addNeighbor(id_new_neighbor, gv.getValue());
        knowledge[myid].clock++;
        calculateLeader();
        Emitter e = (Emitter) host.getProtocol(emitPid);
        KnowledgeMessage knowledgeMessage = new KnowledgeMessage(myid, Emitter.ALL, myPid, knowledge);
        e.emit(host, knowledgeMessage);
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        if (pid != myPid) {
            throw new RuntimeException("Received Event for wrong protocol");
        }
        if (event instanceof Message){
            if (((Message) event).getIdSrc() == myid) return;
        }
        if (event instanceof KnowledgeMessage) {
            knowledgeReception(node, (KnowledgeMessage)event);
        } else if (event instanceof EditMessage) {
            editReception(node, pid, (EditMessage)event);
        } else
            System.err.println("Not a valid event type");
    }

    private void knowledgeReception(Node node, KnowledgeMessage msg) {
        View[] knowledgeJ = msg.getKnowledge();
        ArrayList<Edit> edit = new ArrayList<>();

        for (int id = 0; id < knowledgeJ.length; id++) {
            View peer = knowledgeJ[id];
            if (peer == null || id == myid) continue;
            if (knowledge[id] == null) {
                // e <- {<p,p.neighbors,-,0,p.clock>}
                Edit e = new Edit(id,0, peer.clock);
                e.setAdded(peer.getNeighbors());
                edit.add(e);
                knowledge[id] = new View(peer.clock, peer.getNeighbors());
            } else if (peer.clock > knowledge[id].clock) {
                // p.neighbors \ knowledge[p].neighbors O(n2)?
                Map<Long, Integer> added = mapDifference(peer.getNeighbors(), knowledge[id].getNeighbors());
                // knowledge[p].neighbors \ p.neighbors
                Map<Long, Integer> removed = mapDifference(knowledge[id].getNeighbors(), peer.getNeighbors());
                Edit e = new Edit(id, knowledge[id].clock, peer.clock);
                e.setAdded(added);
                e.setRemoved(removed);
                edit.add(e);
                knowledge[id].setNeighbors(peer.getNeighbors());
                knowledge[id].clock = peer.clock;
            }
        }

        calculateLeader();
        if (!edit.isEmpty()) {
            Emitter e = (Emitter) node.getProtocol(emitPid);
            EditMessage editMsg = new EditMessage(myid, Emitter.ALL, myPid, edit);
            e.emit(node, editMsg);
        }
    }


    private void editReception(Node node, int pid, EditMessage msg) {
        boolean updated = false;
        boolean updatedK = false;
        for(Edit e: msg.getEdit()){
            int source = (int)e.nodeid;
            if(source == myid) continue;
            if (!e.addedIsEmpty()) {
                if (knowledge[source] == null)
                    knowledge[source] = new View(0);

                if ( e.oldclock==0 || knowledge[source].clock == e.oldclock) {
                    // knowledge[source].neighbors U added
                    for (Map.Entry<Long,Integer> entry: e.getAdded().entrySet())
                        updated |= knowledge[source].addNeighbor(entry.getKey(), entry.getValue());
                }
            }

            if (!e.removedIsEmpty()) {
                if (knowledge[source]!= null && knowledge[source].clock == e.oldclock) {
                    // knowledge[source].neighbors \ removed
                    for (Long id: e.getRemoved().keySet()) {
                        updated |= knowledge[source].removeNeighbor(id);
                    }
                }
            }

            if (knowledge[source]!= null && updated){
                knowledge[source].clock = e.newclock;
                updatedK = true;
            }
            updated = false;
        }
        calculateLeader();
        if (!updatedK) return;
        // Knowledge was updated
        Emitter e = (Emitter) node.getProtocol(emitPid);
        for(long i= 0; i< Network.size(); i++) {
            if(i == msg.getIdSrc() || i == node.getID()) continue; // dont send to the sender or ourselves
            EditMessage edit = new EditMessage(myid, i, myPid, msg.getEdit());
            e.emit(node, edit);
        }
    }

    /*
     * HELPERS
     */

    private void calculateLeader() {
        this.leaderval =-1;
        Set<Long> visited = new HashSet<>();
        getHighest(knowledge[myid].getNeighbors(), visited);
    }

    private void getHighest(Map<Long,Integer> neighbors, Set<Long> visited){
        if(neighbors.isEmpty() || neighbors == null) return ;
        for(Map.Entry<Long,Integer> e: neighbors.entrySet()){
            if(e.getValue() > this.leaderval){
                this.leaderval = e.getValue();
                this.leader = e.getKey();
            }
            if(e.getKey() != myid && knowledge[e.getKey().intValue()]!=null && !visited.contains(e.getKey())) {
                visited.add(e.getKey());
                getHighest(knowledge[e.getKey().intValue()].getNeighbors(), visited);
            }
        }
    }

    // Returns a map that contains the values that are in map1 but not in map2
    public Map<Long, Integer> mapDifference(Map<Long, Integer> map1, Map<Long, Integer> map2){
        Map<Long, Integer> diff = new HashMap<>(map1);
        for(Long id: map2.keySet()){
            diff.remove(id);
        }
        return diff;
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

    /*
     * MONITORABLE
     */
    @Override
    public List<String> infos(Node host) {
        List<String> res = new ArrayList<>();
        res.add("Node: " + host.getID());
        GlobalViewElection gv = (GlobalViewElection) host.getProtocol(myPid);
        res.add("Leader: " + gv.getIDLeader());
        return res;
    }

    private void printKnowledge(long id, View[] knowledge){
        for(View v: knowledge){
            if(v == null) continue;
            System.out.println("Node "+id+": "+v.clock+", "+v.getNeighbors().toString());
        }
    }
}
