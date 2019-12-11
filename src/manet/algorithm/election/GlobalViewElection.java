package manet.algorithm.election;

import manet.Monitorable;
import manet.communication.Emitter;
import manet.communication.EmitterImpl;
import manet.detection.NeighborhoodListener;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.ExtendedRandom;

import peersim.config.Configuration;
import util.globalview.Edit;
import util.globalview.EditMessage;
import util.globalview.KnowledgeMessage;
import util.globalview.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalViewElection implements ElectionProtocol, Monitorable, NeighborhoodListener {


    // Protocol configuration variables
    private static final String PAR_NEIGHBORPID = "neighborprotocol";
    private static final String PAR_EMITTERPID = "emitterprotocol";
    private static final String PAR_SEED = "seed_value";
    private final ExtendedRandom valueRandom;
    private final int myPid;
    private int emitPid;
    private int neighborPid;

    // Aux
    private int myid;

    // Election variables
    private long leader;
    private int value;
    private ArrayList<View> knowledge = new ArrayList<View>();

    /*
     *  INITIALIZATION BLOCK
     */
    public GlobalViewElection(String prefix) {
        String tmp[] = prefix.split("\\.");
        myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
        this.valueRandom = new ExtendedRandom(Configuration.getInt(prefix + "." + PAR_SEED));
        emitPid = Configuration.getPid(prefix + "." + PAR_EMITTERPID);
        neighborPid = Configuration.getPid(prefix + "." + PAR_NEIGHBORPID);
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
        election.knowledge = new ArrayList<View>();
        for(View v: election.knowledge)
            v.clock = -1;
        return election;
    }

    public void initSelfKnowledge(long id) {
        myid = (int) id;
        for(View v: knowledge)
            v.clock = -1;
        knowledge.get(myid).clock = 0;
        knowledge.get(myid).addNeighbor(id, this.value);
    }

    /*
     *  EVENTS BLOCK
     */
    public void lostNeighborDetected(Node host, long id_lost_neighbor) {
        Edit edit = new Edit(host.getID(), knowledge.get(myid).clock, knowledge.get(myid).clock+1);
        edit.addRemoved(id_lost_neighbor, knowledge.get(myid).getValue(id_lost_neighbor));
        knowledge.get(myid).removeNeighbor(id_lost_neighbor);
        knowledge.get(myid).clock++;
        Emitter e = (EmitterImpl) host.getProtocol(emitPid);
        EditMessage editMsg = new EditMessage(host.getID(), Emitter.ALL, myPid, edit);
        e.emit(host, editMsg);
    }

    public void newNeighborDetected(Node host, long id_new_neighbor) {
        // is this actually allowed?
        ElectionProtocol gv = (ElectionProtocol) Network.get((int)id_new_neighbor).getProtocol(myPid);
        knowledge.get(myid).addNeighbor(id_new_neighbor, gv.getValue());
        knowledge.get(myid).clock++;
        Emitter e = (EmitterImpl) host.getProtocol(emitPid);
        KnowledgeMessage knowledgeMessage = new KnowledgeMessage(host.getID(), Emitter.ALL, myPid, knowledge);
        e.emit(host, knowledgeMessage);
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        if (pid != myPid) {
            throw new RuntimeException("Receive Event for wrong protocol");
        }
        if (event instanceof KnowledgeMessage) {
            knowledgeReception(node, (KnowledgeMessage)event);
        } else if (event instanceof EditMessage) {
            editReception(node, pid, (EditMessage)event);
        } else
            System.err.println("Not a valid event type");
    }

    private void knowledgeReception(Node node, KnowledgeMessage msg) {
        ArrayList<View> knowledgeJ = msg.getKnowledge();
        long j = msg.getIdSrc();
        ArrayList<Edit> edit = new ArrayList<>();
        // for every node
        for (int id = 0; id < knowledgeJ.size(); id++) {
            View peer = knowledgeJ.get(id);
            if (knowledge.get(id).clock == -1) {
                // e <- {<p,p.neighbors,-,0,p.clock>}
                Edit e = new Edit(id, 0, peer.clock);
                e.setAdded(peer.getNeighbors());
                edit.add(e);
                knowledge.get(id).clock = peer.clock;
                knowledge.get(id).setNeighbors(peer.getNeighbors());
            } else if (peer.clock > knowledge.get(id).clock) {
                //p.neighbors \ knowledge[p].neighbors O(n2)?
                Map<Long, Integer> added = mapDifference(peer.getNeighbors(), knowledge.get(id).getNeighbors());
                // knowledge[p].neighbors \ p.neighbors
                Map<Long, Integer> removed = mapDifference(knowledge.get(id).getNeighbors(), peer.getNeighbors());
                Edit e = new Edit(id, knowledge.get(id).clock, peer.clock);
                e.setAdded(added);
                e.setRemoved(removed);
                edit.add(e);
                knowledge.get(id).setNeighbors(peer.getNeighbors());
                knowledge.get(id).clock = peer.clock;
            }
        }
        if (!edit.isEmpty()) {
            Emitter e = (EmitterImpl) node.getProtocol(emitPid);
            EditMessage editMsg = new EditMessage(node.getID(), Emitter.ALL, myPid, edit);
            e.emit(node, editMsg);
        }
    }

    // Returns a map that contains the values that are in map1 but not in map2
    // XXX: should we also check the value? remove(key,value)
    private Map<Long, Integer> mapDifference(Map<Long, Integer> map1, Map<Long, Integer> map2){
        Map<Long, Integer> diff = new HashMap<Long, Integer>(map1);
        for(Long id: map2.keySet()){
            diff.remove(id);
        }
        return diff;
    }

    private void editReception(Node node, int pid, EditMessage msg) {
        boolean updated = false;
        for(Edit e: msg.getEdit()){
            if (!e.addedIsEmpty()) {
                if (knowledge.get((int) e.nodeid).clock == -1) {
                    if (e.oldclock == 0) {
                        updated = true;
                        knowledge.get((int) e.nodeid).setNeighbors(e.getAdded());
                    }
                } else if (e.oldclock == knowledge.get((int) e.nodeid).clock) {
                    updated = true;
                    knowledge.get((int) e.nodeid).getNeighbors().putAll(e.getAdded());
                }
            }

            if (!e.removedIsEmpty()) {
                if (knowledge.get((int)e.nodeid).clock == e.oldclock) {
                    updated = true;
                        for (Long id: e.getRemoved().keySet())
                            knowledge.get((int)e.nodeid).removeNeighbor(id);
                    }
                }
            }
        if (!updated) return;
        Emitter e = (EmitterImpl) node.getProtocol(emitPid);
        e.emit(node, msg);
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
