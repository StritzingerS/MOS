package at.fhooe.mc.mos.model;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple model of a step for testing purposes.
 */
public class Step {
    private int count;


    public Step() {
        // Default constructor required for calls to DataSnapshot.getValue(ListItem.class)
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("count", count);
        return result;
    }
}
