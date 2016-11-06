package at.fhooe.mc.mos.ui;

/**
 * Interface which defines a view to interact with a pedometer.
 */
public interface PedometerView {
    void currentSteps(int currentSteps);

    void dataSaved(boolean success);
}
