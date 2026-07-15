package com.fakesms.app;

import java.util.Calendar;

public class DateConverter {

    public static long jalaliToMillis(String jalaliDate, String time) {
        try {
            String[] dateParts = jalaliDate.split("/");
            String[] timeParts = time.split(":");

            int jYear = Integer.parseInt(dateParts[0]);
            int jMonth = Integer.parseInt(dateParts[1]);
            int jDay = Integer.parseInt(dateParts[2]);

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            int gYear = jYear + 1600;
            int gMonth = jMonth;
            int gDay = jDay - 1;

            Calendar calendar = Calendar.getInstance();
            calendar.set(gYear, gMonth - 1, gDay, hour, minute, 0);

            return calendar.getTimeInMillis();

        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
