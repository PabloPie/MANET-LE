package manet.communication;

import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import util.Message;
import util.ProbeMessage;
import util.WatcherMessage;

public class EmitterWatcher implements Emitter {

    private static final String PAR_EMITTER = "emitter";
    private final int pidemitter;
	private final int myPid;

    public static int msgSent = 0;
    public static int msgReceived = 0;

    public EmitterWatcher(String prefix) {
    	String tmp[] = prefix.split("\\.");
		myPid = Configuration.lookupPid(tmp[tmp.length - 1]);
        pidemitter = Configuration.lookupPid(PAR_EMITTER);
    }

    @Override
    public Object clone() {
        manet.communication.EmitterWatcher res = null;
        try {
            res = (manet.communication.EmitterWatcher) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return res;
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        if(!(event instanceof ProbeMessage)) msgReceived++;
		if(event instanceof WatcherMessage){
		  Message msg = ((WatcherMessage) event).getMessage();
          EDSimulator.add(0, msg, node, msg.getPid());
        }
    }

    @Override
    public void emit(Node host, Message msg) {
        Emitter emitter = (Emitter) host.getProtocol(pidemitter);
        if(!(msg instanceof ProbeMessage)) msgSent++;
        WatcherMessage wmsg = new WatcherMessage(msg.getIdSrc(), msg.getIdDest(), myPid, msg);
        emitter.emit(host, wmsg);
    }

    @Override
    public int getLatency() {
        return 0;
    }

    @Override
    public int getScope() {
        return 0;
    }
   
}
