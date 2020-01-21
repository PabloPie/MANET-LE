package util.globalview;

import util.Message;

import java.util.ArrayList;
import java.util.List;

public class EditMessage extends Message {

    private final List<Edit> edit;

    public EditMessage(long idsrc, long iddest, int pid, List<Edit> edit) {
        super(idsrc, iddest, pid);
        this.edit = edit;
    }

    public EditMessage(long idsrc, long iddest, int pid, Edit edit) {
        super(idsrc, iddest, pid);
        this.edit = new ArrayList<>();
        this.edit.add(edit);
    }

    public List<Edit> getEdit() {
        return this.edit;
    }

}
