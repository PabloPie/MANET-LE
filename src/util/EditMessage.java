package util;

import manet.algorithm.election.GlobalViewElection;

import java.util.ArrayList;

public class EditMessage extends Message {

    public class Edit {
        public long nodeid;
        public GlobalViewElection.Peer[] added;
        public GlobalViewElection.Peer[] removed;
        public int oldclock;
        public int newclock;
    }

    private final ArrayList<Edit> edit;

    public EditMessage(long idsrc, long iddest, int pid ) {
        super(idsrc, iddest, pid);
        edit = null;
    }
}
