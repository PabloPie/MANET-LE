package util;

public class LeaderMessage extends Message {
	private int maxValue;
	private long maxNodeId;
	public LeaderMessage(long idsrc, long iddest, int pid, int maxValue, long maxNodeId) {
		super(idsrc, iddest, pid);
		this.maxValue = maxValue;
		this.maxNodeId = maxNodeId;
	}
	
	public int getMaxValue() {
		return this.maxValue;
	}
	
	public long getMaxNodeId() {
		return this.maxNodeId;
	}

}
