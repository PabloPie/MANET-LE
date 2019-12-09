package util.globalview;

import util.Message;

import java.util.ArrayList;

public class KnowledgeMessage extends Message {

    private ArrayList<View> knowledge;

    public KnowledgeMessage(long idsrc, long iddest, int pid, ArrayList<View> knowledge){
        super(idsrc, iddest, pid);
        this.knowledge = knowledge;
    }

    public ArrayList<View> getKnowledge(){
        return this.knowledge;
    }
}
