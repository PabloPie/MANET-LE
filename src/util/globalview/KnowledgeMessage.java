package util.globalview;

import util.Message;

import java.util.ArrayList;

public class KnowledgeMessage extends Message {

    private View[] knowledge;

    public KnowledgeMessage(long idsrc, long iddest, int pid, View[] knowledge){
        super(idsrc, iddest, pid);
        this.knowledge = knowledge.clone();
    }

    public View[] getKnowledge(){
        return this.knowledge;
    }
}
