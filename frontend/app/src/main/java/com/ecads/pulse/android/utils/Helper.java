package com.ecads.pulse.android.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Helper {

    public static final String HOSPITAL = "hospital";
    public static final String CONSULTANT = "consultant";

    private static DateTimeFormatter inputFormat =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static DateTimeFormatter outputFormat =
            DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    public static String formatDate(String date) {
        LocalDate formatedInput = LocalDate.parse(date, inputFormat);
        if (LocalDate.now().equals(formatedInput)) {
            return "Today";
        }
        return formatedInput.format(outputFormat);
    }
}
