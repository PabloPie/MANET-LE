package util;

public class ElectionMessage extends Message {
	private final Pair<Integer, Long> id;
	public ElectionMessage(long idsrc, long iddest, int pid, Pair<Integer, Long> id) {
		super(idsrc, iddest, pid);
		this.id = id;
	}
	
	public Pair<Integer, Long> getElectionId() {
		return this.id;
	}

}