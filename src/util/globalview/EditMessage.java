package util.globalview;

import util.Message;

import java.util.ArrayList;

public class EditMessage extends Message {

    private final ArrayList<Edit> edit;

    public EditMessage(long idsrc, long iddest, int pid, ArrayList<Edit> edit) {
        super(idsrc, iddest, pid);
        this.edit = new ArrayList<>();
        for(Edit e: edit) {
            this.edit.add(new Edit(e));
        }
    }

    public EditMessage(long idsrc, long iddest, int pid, Edit edit) {
        super(idsrc, iddest, pid);
        this.edit = new ArrayList<>();
        this.edit.add(new Edit(edit));
    }

    public EditMessage(long idsrc, long iddest, int pid) {
        super(idsrc, iddest, pid);
        this.edit = new ArrayList<>();
    }

    public void addEdit(Edit edit) {
        this.edit.add(new Edit(edit));
    }

    public ArrayList<Edit> getEdit() {
        return this.edit;
    }

}
