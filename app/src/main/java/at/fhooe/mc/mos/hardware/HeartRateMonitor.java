package at.fhooe.mc.mos.hardware;

import at.fhooe.mc.mos.logic.HeartRateObserver;

/**
 * Created by Oliver on 10.11.2016.
 */
public interface HeartRateMonitor {
    void addObserver(HeartRateObserver observer);
    void removeObserver(HeartRateObserver observer);
}
