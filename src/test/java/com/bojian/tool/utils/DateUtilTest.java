package com.bojian.tool.utils;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.util.Date;

@SpringBootTest
public class DateUtilTest {

    @Test
    public void stringToDate() {
        String beginTime = "2014-08-15 10:22:22";
        Date date = DateUtil.stringToDate(beginTime);
        System.out.println("date = " + date);
    }

    @Test
    public void dateToString() {
        String StringDate = DateUtil.dateToString(new Date());
        System.out.println("StringDate = " + StringDate);
    }

    @Test
    public void timestampToString() {
        long date = new Date().getTime();
        String string = DateUtil.timestampToString(date);
        System.out.println("string = " + string);
    }

    @Test
    public void stringToTimestamp() {
        Timestamp timestamp = DateUtil.dateToTimestamp(new Date());
        System.out.println("timestamp = " + timestamp);
    }
}