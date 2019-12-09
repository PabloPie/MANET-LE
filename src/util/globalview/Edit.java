package util.globalview;

import java.util.HashMap;
import java.util.Map;

public class Edit {

        public long nodeid;
        private Map<Long, Integer> added;
        private Map<Long, Integer> removed;
        public int oldclock;
        public int newclock;

        public Edit(long id, int oldclock, int newclock){
                nodeid = id;
                this.oldclock = oldclock;
                this.newclock = newclock;
                added = new HashMap<>();
                removed = new HashMap<>();
        }

        public void addRemoved(long nodeid, int value){
                removed.putIfAbsent(nodeid, value);
        }

        public void addAdded(long nodeid, int value){
                added.putIfAbsent(nodeid, value);
        }

        public void setRemoved(Map<Long, Integer> removed) {
                this.removed = removed;
        }

        public void setAdded(Map<Long, Integer> added) {
                this.added = added;
        }
}
