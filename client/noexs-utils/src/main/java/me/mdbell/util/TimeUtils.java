package me.mdbell.util;

public class TimeUtils {

    private static final long SECOND_IN_MS = 1000;
    private static final long MINUTE_IN_MS = SECOND_IN_MS * 60;
    private static final long HOUR_IN_MS = MINUTE_IN_MS * 60;

    public static String formatTime(long timeInMs) {
        StringBuilder sb = new StringBuilder();
        long tmp = timeInMs / HOUR_IN_MS;
        if(tmp > 0) {
            if(tmp < 10) {
                sb.append('0');
            }
            sb.append(tmp).append(':');
            timeInMs -= HOUR_IN_MS * tmp;
        }
        tmp = timeInMs / MINUTE_IN_MS;
        if(tmp < 10) {
            sb.append('0');
        }
        sb.append(tmp).append(':');
        timeInMs -= MINUTE_IN_MS * tmp;

        tmp = timeInMs / SECOND_IN_MS;
        if(tmp < 10) {
            sb.append('0');
        }
        sb.append(tmp);
        return sb.toString();
    }
}
