package util;

public class WatcherMessage extends Message {
	private final Message msg;
	public WatcherMessage(long idsrc, long iddest, int pid, Message msg) {
		super(idsrc, iddest, pid);
		this.msg =msg;
	}
	public Message getMessage() { return this.msg; }
}