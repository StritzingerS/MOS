package at.fhooe.mc.mos.ui;

/**
 * Interface which defines a view to interact with a pedometer.
 */
public interface PedometerView {
    void currentSteps(int currentSteps);
    void currentDistance(double currentDistance);
    void currentPace(float currentPace);
    void currentCalories(int currentCalories);
    void currentEquivalentDistance(double currentEquivalentDistance);
    void currentEquivalentPace(float currentEquivalentPace);

    void dataSaved(boolean success);
}
