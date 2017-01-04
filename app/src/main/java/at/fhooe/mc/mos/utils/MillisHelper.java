package at.fhooe.mc.mos.utils;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Oliver on 04.01.2017.
 */
public class MillisHelper {
    public static String millisToLocaleString(long milliseconds) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(milliseconds));
    }

    public static String millisToDuration(long milliseconds) {
        int seconds = (int) (milliseconds / 1000) % 60 ;
        int minutes = (int) ((milliseconds / (1000*60)) % 60);
        int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
        return String.valueOf(hours) + ":" + String.valueOf(minutes) + ":" + String.valueOf(seconds);
    }
}
