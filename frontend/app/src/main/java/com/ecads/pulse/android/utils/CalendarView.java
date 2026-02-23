package com.ecads.pulse.android.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;

public class CalendarView {
    public static void setCalendarView(Context context, EditText textField, boolean moveBack) {
        Calendar calendar = Calendar.getInstance();
        if(moveBack){
            calendar.add(Calendar.YEAR, -20);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);

        DatePickerDialog dialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {
//                        String dob = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        String dob = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);;
                        textField.setText(dob);
                    }
                },
                year, month, date
        );

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }
}
