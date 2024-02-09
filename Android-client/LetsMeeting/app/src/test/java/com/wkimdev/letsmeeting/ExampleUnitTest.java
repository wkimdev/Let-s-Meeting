package com.wkimdev.letsmeeting;

import org.junit.Test;

import static org.junit.Assert.*;

import com.wkimdev.letsmeeting.util.CommonUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void setTest() {
        LocalDateTime now = LocalDateTime.now();

        // 지정된 패턴을 사용해 포맷터를 만든다
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM월 dd일(E)");

        DateTimeFormatter dateTimeFormatter2 = DateTimeFormatter.ofPattern("YYYY-MM-dd E HH:mm");

        // 지정된 포맷터를 사용해 날짜/시간을 포맷한다
        System.out.println("첫번째 >>>> " + now.format(dateTimeFormatter));
        System.out.println("두번째 >>>> " + now.format(dateTimeFormatter2));
    }
}