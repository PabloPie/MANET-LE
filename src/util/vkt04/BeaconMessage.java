package util.vkt04;

import util.Message;

public class BeaconMessage extends Message {
	private final Pair<Integer, Long> leader;
	private final long timestamp;
	public BeaconMessage(long idsrc, long iddest, int pid, Pair<Integer, Long> leader, long timestamp) {
		super(idsrc, iddest, pid);
		this.leader = leader;
		this.timestamp = timestamp;
	}
	
	public Pair<Integer, Long> getLeader() {
		return this.leader;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

}
