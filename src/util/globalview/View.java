package util.globalview;

import java.util.HashMap;
import java.util.Map;

public class View{
    public int clock;
    private Map<Long, Integer> neighbors;

    public View(int clock){
        this.clock = clock;
        this.neighbors = new HashMap<>();
    }

    public View(View copy) {
        if (copy == null) throw new NullPointerException("View is null");
        this.clock = copy.clock;
        this.neighbors = new HashMap<>(copy.neighbors);
    }

    public boolean addNeighbor(long id, int value) {
        // putIfAbsent returns null when it is added
        // if we assume node values don't change we could just use put
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
        this.neighbors = new HashMap<>(neighbors);
    }
}

