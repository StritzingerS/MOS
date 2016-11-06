package at.fhooe.mc.mos.logic;

/**
 * Interface to notify any observer about the pedometer.
 */
public interface PedometerObserver {
    void stepDetected();
}
