package at.fhooe.mc.mos.ui;

/**
 * Created by Oliver on 10.11.2016.
 */
public interface HeartRateView {
    void currentHeartRate(int heartRate);
    void currentCalories(int currentCalories);
    void currentAvgHeartRate(int avgHeartRate);
    void currentHrMaxPercentage(int hrMaxPercentage);
}
