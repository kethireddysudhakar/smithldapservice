package com.smithdrug.sls.util;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class slsUtil {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd-HH.mm.ss";
    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
	
	public static String getCurrentDateTime()
	{
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		Date date = new Date();
		String returnDate = dateFormat.format(date);
		return returnDate;
	}
	
	public static String getNextDateTime(String date)
	{
		// Get current date
        Date currentDate = null;
		try {
			currentDate = new SimpleDateFormat(DATE_FORMAT).parse(date);
		} catch (ParseException e) {
			System.out.println("fpsUtil.getNextDateTime() Date Error ");
			e.printStackTrace();
		}

        // convert date to localdatetime
        LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        localDateTime = localDateTime.plusDays(1);
        // plus one
        // convert LocalDateTime to date
        Date currentDatePlusOneDay = currentDate.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

        return dateFormat.format(currentDatePlusOneDay);
	}
	
	public static boolean isDateExpired(Timestamp checkDate)
	{
		if(!checkDate.before(new Timestamp(System.currentTimeMillis())))
		{
			return true;
		}
		return false;
	}

}
