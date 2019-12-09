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
        return election;
    }

    public void initSelfKnowledge(long id) {
        myid = (int) id;
        knowledge.get(myid).clock = 0;
        knowledge.get(myid).addNeighbor(id, this.value);
    }

    /*
     *  EVENTS BLOCK
     */
    public void lostNeighborDetected(Node host, long id_lost_neighbor) {
        Edit edit = new Edit(host.getID(), knowledge.get(myid).clock, knowledge.get(myid).clock+1);
        edit.addRemoved(id_lost_neighbor, knowledge.get(myid).getValue(id_lost_neighbor));
        EditMessage editMsg = new EditMessage(host.getID(), Emitter.ALL, myPid, edit);
        knowledge.get(myid).removeNeighbor(id_lost_neighbor);
        knowledge.get(myid).clock++;
        Emitter e = (EmitterImpl) host.getProtocol(emitPid);
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
        for(View v: knowledgeJ) {
            //v.getNeighbors().forEach((id, val) ->
                   //IF KNOWLEDGE[p] is empty);
        }
    }

    private void editReception(Node node, int pid, EditMessage msg) {

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
