package at.fhooe.mc.mos.hardware;

import at.fhooe.mc.mos.logic.AltitudeObserver;

/**
 * Interface for an altitude monitor.
 */
public interface AltitudeMonitor {
    void addObserver(AltitudeObserver observer);
    void removeObserver(AltitudeObserver observer);
}
