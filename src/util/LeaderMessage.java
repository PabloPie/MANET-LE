package util;

public class LeaderMessage extends Message {
	private final int maxValue;
	private final long maxNodeId;
	private final Pair<Integer, Long> id;
	public LeaderMessage(long idsrc, long iddest, int pid, Pair<Integer, Long> id, int maxValue, long maxNodeId) {
		super(idsrc, iddest, pid);
		this.maxValue = maxValue;
		this.maxNodeId = maxNodeId;
		this.id = id;
	}
	
	public int getMaxValue() {
		return this.maxValue;
	}
	
	public long getMaxNodeId() {
		return this.maxNodeId;
	}
	
	public Pair<Integer, Long> getElectionId() {
		return this.id;
	}

}
