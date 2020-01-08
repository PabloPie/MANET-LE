package manet.detection;

import java.util.List;

import peersim.core.Protocol;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface NeighborProtocol extends Protocol {

	/* Renvoie la liste courante des Id des voisins directs */
	public List<Long> getNeighbors();
	
	public void initialiseNeighbors(List<Long> neighbors);
	
}
