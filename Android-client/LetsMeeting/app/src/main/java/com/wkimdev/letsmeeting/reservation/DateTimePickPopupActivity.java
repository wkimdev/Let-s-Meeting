package com.wkimdev.letsmeeting.reservation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog;
import com.wkimdev.letsmeeting.R;

import java.util.Date;
import java.util.Locale;

//날짜와 시간을 같이 선택할 수 있는 팝업
public class DateTimePickPopupActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private SingleDateAndTimePicker picker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_time_pick_popup);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new SingleDateAndTimePickerDialog.Builder(DateTimePickPopupActivity.this)
                .customLocale(Locale.KOREAN)
                .todayText("오늘")
                .displayListener(new SingleDateAndTimePickerDialog.DisplayListener() {
                    @Override
                    public void onDisplayed(SingleDateAndTimePicker picker) {

                    }
                })
                .title("회의날짜선택")
                .listener(new SingleDateAndTimePickerDialog.Listener() {
                    @Override
                    public void onDateSelected(Date date) {
                        Log.e(TAG, "onDateSelected: "+ date);


                    }
                }).display();
    }
}