package com.ecads.pulse.android.fragment;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.LoginActivity;
import com.ecads.pulse.android.databinding.FragmentReg01Binding;
import com.ecads.pulse.android.utils.CalendarView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Reg01Fragment extends Fragment {

    private FragmentReg01Binding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReg01Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String mobileValidationRegex = "^(?:\\+94|0)?7[0124578]\\d{7}$";

        //login btn
        TextView loginLink = binding.reg1LoginLink;
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), LoginActivity.class));
                requireActivity().finish();
            }
        });

        //calendar view
        EditText dateOfBirthField = binding.dobField;
        dateOfBirthField.setOnClickListener(v -> {
            CalendarView.setCalendarView(view.getContext(), dateOfBirthField, true);
        });

        // gender dropdown
        AutoCompleteTextView genderField = binding.genderField;
        String[] genders = {"Select your gender", "MALE", "FEMALE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_dropdown_item_1line, genders);
        genderField.setAdapter(adapter);
        genderField.setText(genders[0], false);

        Button nextBtn = binding.reg1BtnNext;
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String fname = binding.firstNameField.getText().toString();
                TextView fnameError = binding.fnameError;

                String lname = binding.lastNameField.getText().toString();
                TextView lnameError = binding.lnameError;

                String mobile = binding.mobileField.getText().toString();
                TextView mobileError = binding.mobileError;

//                EditText dob = binding.dobField;
                TextView dobError = binding.dobError;

//                EditText gender = binding.genderField;
                TextView genderError = binding.genderError;

                List<TextView> errorList = Arrays.asList(fnameError, lnameError, mobileError, dobError, genderError);

                for (TextView error : errorList) {
                    error.setVisibility(GONE);
                }

                if (fname.isEmpty()) {
                    fnameError.setVisibility(VISIBLE);
                    return;
                }

                if (lname.isEmpty()) {
                    lnameError.setVisibility(VISIBLE);
                    return;
                }

                if (mobile.isEmpty()) {
                    mobileError.setVisibility(VISIBLE);
                    return;
                }

                if (!Patterns.PHONE.matcher(mobile).matches() ||
                        !mobile.matches(mobileValidationRegex)) {
                    mobileError.setVisibility(VISIBLE);
                    mobileError.setText("* Enter a valid mobile number.");
                    return;
                }

                if (dateOfBirthField.getText().toString().isEmpty()) {
                    dobError.setVisibility(VISIBLE);
                    return;
                }

                if (LocalDate.parse(dateOfBirthField.getText().toString(), DateTimeFormatter.ofPattern("dd/MM/yyyy")).getYear() ==
                        LocalDate.now().getYear()) {
                    dobError.setVisibility(VISIBLE);
                    dobError.setText("* Select a valid date of birth.");
                    return;
                }

                if (genderField.getText().toString().equals(genders[0])) {
                    genderError.setVisibility(VISIBLE);
                    return;
                }

                Bundle dataBundle = new Bundle();
                dataBundle.putString("fname", fname);
                dataBundle.putString("lname", lname);
                dataBundle.putString("mobile", mobile);
                dataBundle.putString("dob", dateOfBirthField.getText().toString());
                dataBundle.putString("gender", genderField.getText().toString());

                Reg02Fragment fragment = new Reg02Fragment();
                fragment.setArguments(dataBundle);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.regFragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();

            }
        });
    }

    private void setCalendarView(Context context, EditText dateOfBirthField) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -20);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);

        DatePickerDialog dialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {
                        String dob = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        dateOfBirthField.setText(dob);
                    }
                },
                year, month, date
        );

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }
}