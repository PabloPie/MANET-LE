package util.globalview;

import java.util.HashMap;
import java.util.Map;

public class View {
    public int clock;
    //separate node -> value in a map, and set of neighbors?
    private Map<Long, Integer> neighbors = new HashMap<>();

    public void addNeighbor(long id, int value){
        neighbors.put(id, value);
    }

    public void removeNeighbor(long id){
        neighbors.remove(id);
    }

    public int getValue(long id){
        return neighbors.get(id);
    }

    public Map<Long, Integer> getNeighbors(){
        return this.neighbors;
    }

    public void setNeighbors(Map<Long, Integer> neighbors){
        this.neighbors = neighbors;
    }

}
