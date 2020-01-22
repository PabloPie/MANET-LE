package util.vkt04;

import util.Message;

public class AckMessage extends Message {
	private final Pair<Integer, Long> maxNode;
	private final Pair<Integer, Long> id;
	public AckMessage(long idsrc, long iddest, int pid, Pair<Integer, Long> id, Pair<Integer, Long> maxNode) {
		super(idsrc, iddest, pid);
		this.maxNode = maxNode;
		this.id = id;
	}
	
	public Pair<Integer, Long> getMaxNode() {
		return this.maxNode;
	}

	public Pair<Integer, Long> getElectionId() {
		return this.id;
	}

}
