package com.mymmall.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

public class DateTimeUtil {

    public final static String STANDARD_TIME = "yyyy-MM-dd HH:mm:ss";

    public static Date strToDate(String dateTimeStr,String formatStr){
        DateTimeFormatter dataTimeFormatter = DateTimeFormat.forPattern(formatStr);
        DateTime dateTime = dataTimeFormatter.parseDateTime(dateTimeStr);
        return dateTime.toDate();
    }
    public static String dateToStr(Date date,String formatStr){
        if (date == null){
            return StringUtils.EMPTY;
        }
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(formatStr);
    }
    public static Date strToDate(String dateTimeStr){
        DateTimeFormatter dataTimeFormatter = DateTimeFormat.forPattern(STANDARD_TIME);
        DateTime dateTime = dataTimeFormatter.parseDateTime(dateTimeStr);
        return dateTime.toDate();
    }
    public static String dateToStr(Date date){
        if (date == null){
            return StringUtils.EMPTY;
        }
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(STANDARD_TIME);
    }
}
