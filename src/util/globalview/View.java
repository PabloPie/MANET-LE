package util.globalview;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class View {
    public int clock;
    //separate node -> value in a map, and set of neighbors?
    private Map<Long, Integer> neighbors;

    public View(int clock){
        this.clock = clock;
        this.neighbors = new HashMap<>();
    }

    public View(int clock, Map<Long, Integer> neighbors){
        this.neighbors = neighbors;
        this.clock = clock;
    }

    public boolean addNeighbor(long id, int value) {
        // putIfAbsent returns null when it is added
        return neighbors.putIfAbsent(id, value)==null;
    }

    public boolean removeNeighbor(long id) {
        // remove returns a value different from null if the value was removed
        return neighbors.remove(id)!=null;
    }

    public int getValue(long id) {
        return neighbors.get(id);
    }

    public Map<Long, Integer> getNeighbors() {
        return this.neighbors;
    }

    public void setNeighbors(Map<Long, Integer> neighbors) {
        this.neighbors = neighbors;
    }
}

