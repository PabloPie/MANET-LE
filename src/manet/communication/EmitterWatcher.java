package manet.communication;

import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import util.Message;
import util.ProbeMessage;

public class EmitterWatcher implements Emitter {


    private static final String PAR_EMITTER = "emitter";
    private final int pidemitter;

    public static int count = 0;
    public static int countTotal = 0;

    public EmitterWatcher(String prefix) {
        String tmp[] = prefix.split("\\.");
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
		if(event instanceof Message){
		  Message msg = (Message) event;
            EDSimulator.add(0, event, node, msg.getPid());
        }
    }


    @Override
    public void emit(Node host, Message msg) {
        Emitter emitter = (Emitter) host.getProtocol(pidemitter);
        countTotal++;
        if(!(msg instanceof ProbeMessage)) count++;
        emitter.emit(host, msg);
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
