package util.globalview;

import util.Message;

import java.util.ArrayList;

public class KnowledgeMessage extends Message {

    private View[] knowledge;

    public KnowledgeMessage(long idsrc, long iddest, int pid, View[] knowledge){
        super(idsrc, iddest, pid);
        this.knowledge = new View[knowledge.length];
        for (int i = 0; i < knowledge.length; i++)
            if(knowledge[i] != null)
                this.knowledge[i] = new View(knowledge[i]);
    }

    public View[] getKnowledge(){
        return this.knowledge;
    }
}
