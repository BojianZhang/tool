package com.bojian.tool.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zbj
 * @version 0.1
 * @application 日期转换及时间比较大小
 * @time 2021-10-28-10:15
 */
public class DateUtil {

    /**
     * String 转换为 Date
     * @param stringDate String 类型的时间
     * @return Date类型的时间
     */
    public static Date stringToDate(String stringDate){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(stringDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Date 转换为 String
     * @param date Date 类型的时间
     * @return String 类型的时间
     */
    public static String dateToString(Date date){

        //2021-10-28 10:31:53
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String stringDate = format.format(date);

        //21-10-28
        format = DateFormat.getDateInstance(DateFormat.SHORT);
        stringDate = format.format(date);

        //2021-10-28
        format = DateFormat.getDateInstance(DateFormat.MEDIUM);
        stringDate = format.format(date);

        //2021年10月28日 星期四
        format = DateFormat.getDateInstance(DateFormat.FULL);
        stringDate = format.format(date);
        return stringDate;
    }


    /**
     * Timestamp转换为String
     * @param timestamp Timestamp 时间类型
     * @return String 类型时间
     */
    public static String timestampToString(Long timestamp){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        String stringDate = format.format(timestamp);
        return stringDate;
    }

    /**
     * Date转换为Timestamp
     * @param date Date 类型的时间
     * @return Timestamp 类型的时间
     */
    public static Timestamp dateToTimestamp(Date date){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(date);
        Timestamp timestamp = Timestamp.valueOf(time);
        return timestamp;
    }
}
