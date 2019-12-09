package util.globalview;

import manet.algorithm.election.GlobalViewElection;
import util.Message;

import java.util.ArrayList;

public class EditMessage extends Message {

    private final ArrayList<Edit> edit;

    public EditMessage(long idsrc, long iddest, int pid, ArrayList<Edit> edit) {
        super(idsrc, iddest, pid);
        this.edit = edit;
    }

    public EditMessage(long idsrc, long iddest, int pid, Edit edit) {
        super(idsrc, iddest, pid);
        this.edit = new ArrayList<>();
        this.edit.add(edit);
    }

    public EditMessage(long idsrc, long iddest, int pid) {
        super(idsrc, iddest, pid);
        this.edit = new ArrayList<>();
    }

    public void addEdit(Edit edit) {
        this.edit.add(edit);
    }

    public ArrayList<Edit> getEdit() {
        return this.edit;
    }

}
