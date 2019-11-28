package tmeBroadcast;
import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class HelloProtocol implements EDProtocol {

	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_MAXSIZELIST = "maxsizelist";
	private final int pid_transport;
	private final int maxsizelist;

	private final int mypid;// pour stocker l’identifiant du protocole
	private List<Integer> mylist; // pour la liste propre àchaque noeud
	private boolean deja_dit_bonjour = false;// pour indiquer si on a déjà envoyer les messages

	public HelloProtocol(String prefix) {
		String tmp[] = prefix.split("\\.");
		mypid = Configuration.lookupPid(tmp[tmp.length - 1]);
		pid_transport = Configuration.getPid(prefix + "." + PAR_TRANSPORT);
		maxsizelist = Configuration.getInt(prefix + "." + PAR_MAXSIZELIST);
		mylist = new ArrayList<>();
	}

	public Object clone() {
		HelloProtocol ap = null;
		try {
			ap = (HelloProtocol) super.clone();
			ap.mylist = new ArrayList<>();
		} catch (CloneNotSupportedException e) {
		}
		return ap;
	}

	public void direBonjour(Node host) {
		Transport tr = (Transport) host.getProtocol(pid_transport);
		for (int i = 0; i < Network.size(); i++) {
			Node dest = Network.get(i);
			Message mess = new HelloMessage(host.getID(), dest.getID(), mypid, new ArrayList<>(mylist));
			tr.send(host, dest, mess, mypid);
		}
		deja_dit_bonjour = true;
	}

	private void receiveHelloMessage(Node host, HelloMessage mess) {
		System.out.println(
				"Noeud " + host.getID() + " : recu Hello de " + mess.getIdSrc() + "sa liste = " + mess.getInfo());
		if (!deja_dit_bonjour) {
			direBonjour(host);
		}
	}

	public void initialisation(Node host) {
		int size_list = CommonState.r.nextInt(maxsizelist);
		for (int i = 0; i < size_list; i++) {
			mylist.add(CommonState.r.nextInt(128));
		}
		if (host.getID() == 0) {
			direBonjour(host);
		}
	}

	@Override
	public void processEvent(Node host, int pid, Object event) {
		if (pid != mypid)
			throw new IllegalArgumentException("Incohérence sur l’identifiant de protocole");
		if (event instanceof HelloMessage) {
			receiveHelloMessage(host, (HelloMessage) event);
		} else {
			throw new IllegalArgumentException("Evenement inconnu pour ce protocole");
		}
	}

}
