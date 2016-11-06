package at.fhooe.mc.mos.hardware;

import at.fhooe.mc.mos.logic.PedometerObserver;

/**
 * Interface which defines a pedometer.
 */
public interface Pedometer {
    void addObserver(PedometerObserver observer);
    void removeObserver(PedometerObserver observer);
}
