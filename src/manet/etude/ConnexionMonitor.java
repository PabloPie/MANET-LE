package manet.etude;

import manet.communication.EmitterWatcher;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Node;
import peersim.util.IncrementalStats;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ConnexionMonitor implements Control{

    private static final String PAR_POSITIONPID = "positionprotocol";
    private static final String PAR_SCOPE = "scope";

    private final int scope;
    private final int position_pid;
    private static final Logger LOGGER = Logger.getLogger(ConnexionMonitor.class.getName());
    static{ System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");}

    // stats
    private IncrementalStats stats;

    public ConnexionMonitor(String prefix) {
        position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
        scope = Configuration.getInt("protocol.emitter." + PAR_SCOPE);
        stats = new IncrementalStats();
    }

    @Override
    public boolean execute() {
        Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);
        Map<Integer, Set<Node>> connexions = PositionProtocol.getConnectedComponents(positions, scope);
        stats.add(connexions.size());
        LOGGER.info("Connected components: "+ connexions.size());
        LOGGER.info("Average: "+ stats.getAverage());
        LOGGER.info("Variance: "+ stats.getVar());
        LOGGER.info("Messages: "+ EmitterWatcher.count);
        LOGGER.info("Total Messages: "+ EmitterWatcher.countTotal);
        return false;
    }
}
