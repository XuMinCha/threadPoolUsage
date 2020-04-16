package com.myApp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;


/**
 * 日期处理类
 *
 */
public class DateUtils {
    // 默认的时间格式

    /** yyyy-MM-dd */
    public static final String DEFUALT_SHOT_TIME_FORMAT = "yyyy-MM-dd";
    /** yyyy-MM-dd */
    public static final String SLASH_SHOT_TIME_FORMAT   = "yyyy/MM/dd";
    /** yyyyMMdd */
    public static final String SHOT_DATE_FORMAT         = "yyyyMMdd";
    /** yyMMdd */
    public static final String SIMPLE_SHOT_DATE_FORMAT  = "yyMMdd";
    /** yyyy-MM-dd HH:mm:ss */
    public static final String DEFUALT_LONG_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /** yyyy-MM-dd HH:mm:ss.SSS */
    public static final String DEFUALT_LONG_TIME_FORMAT_MILLI = "yyyy-MM-dd HH:mm:ss.SSS";
    
    /** HH:mm:ss */
    public static final String LONG_TIME_FORMAT         = "HH:mm:ss";
    /** HHmmss */
    public static final String SHOT_TIME_FORMAT         = "HHmmss";
    
    public static final String SLASH_TIME_FORMAT        = "yyyy/MM/dd HH:mm:ss";
    
	public static boolean monthEnd(String date) throws ParseException {
		if(date.equals(getEndDateOfMonth(date))) return true;
		return false;
	}
	
	/**
	 * 得到月底 
	 */
	public static String getEndDateOfMonth(String curDate) throws ParseException{
		if(curDate == null || curDate.length() !=10) return null;
		curDate = curDate.substring(0, 8) + "01";
        Calendar cal  = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        cal.setTime(formatter.parse(curDate));
        int maxDays = cal.getActualMaximum(Calendar.DATE);//得到该月的最大天数
        cal.set(Calendar.DATE,maxDays);
        return formatter.format(cal.getTime());
    }

	public static Date getDate(String curDate) throws ParseException{
        Calendar cal  = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        cal.setTime(formatter.parse(curDate));
        
        return cal.getTime();
    }
	
	public static Date getDate(String curDate,String format) throws ParseException{
        Calendar cal  = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        cal.setTime(formatter.parse(curDate));
        
        return cal.getTime();
    }
	
	public static String getToday() throws ParseException{
		Date curDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        return formatter.format(curDate);
    }
	
	/**
	 * 得到当前的时间 返回值:HH:MM:SS
	 */
	public static String getDateTime(Date date,String format){
		SimpleDateFormat sdfTempDate = new SimpleDateFormat(format);
		String prev = sdfTempDate.format(date);
		return prev;
	}
	
	/**
	 * 得到当前的日期 返回值:YYYYMMDD
	 */
	public static String getStringDate(String date) throws ParseException{
        return date.substring(0,4)+date.substring(5,7)+date.substring(8,10);
    }
	/**
	 * @param date  yyyy/MM/dd
	 * @param type  1 返回数字  2 返回中文 3 返回英文
	 * 返回星期  
	 * 1 星期一 、2 星期二 、3星期三、4 星期四、5 星期五、6 星期六、7 星期日
	 */
	public static String getWeekDay(String date,String type) throws ParseException{
		String[] sWeekDates = {"星期日","星期一","星期二","星期三","星期四","星期五","星期六"};
		String[] sWeekDatesE = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		cal.setTime(formatter.parse(date));
		if(type.equals("2")) 
			return sWeekDates[cal.get(Calendar.DAY_OF_WEEK)-1];
		else if(type.equals("3")) 
			return sWeekDatesE[cal.get(Calendar.DAY_OF_WEEK)-1];
		else
			return String.valueOf(cal.get(Calendar.DAY_OF_WEEK)-1);
    }
	
	
	/**
	 * @param date  yyyy/MM/dd
	 * @return  yyyy/MM/dd
	 * 返回某个日期对应周的所有日期
	 */
	public static String[] getWeekDates(String date) throws ParseException{
		String[] sWeekDates = {"","","","","","",""};
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		cal.setTime(formatter.parse(date));
		//星期日
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK)-6);
		sWeekDates[0]=formatter.format(cal.getTime());
		//星期一
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK)-5);
		sWeekDates[1]=formatter.format(cal.getTime());
		//星期二
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK)-4);
		sWeekDates[2]=formatter.format(cal.getTime());
		//星期三
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK)-3);
		sWeekDates[3]=formatter.format(cal.getTime());
		//星期四
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK)-2);
		sWeekDates[4]=formatter.format(cal.getTime());
		//星期五
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK)-1);
		sWeekDates[5]=formatter.format(cal.getTime());
		//星期六
		cal.add(Calendar.DATE, 7-cal.get(Calendar.DAY_OF_WEEK));
		sWeekDates[6]=formatter.format(cal.getTime());
        return sWeekDates;
    }
	/**
	 * @param beginDate
	 * @param endDate
	 * @return
	 * @throws ParseException
	 * 获取两个日期之间的天数
	 */	
	public static int getDays(String sBeginDate,String sEndDate){
		Date startDate = java.sql.Date.valueOf(sBeginDate.replace('/', '-'));
		Date endDate = java.sql.Date.valueOf(sEndDate.replace('/', '-'));

		int iDays = (int) ((endDate.getTime() - startDate.getTime()) / 86400000L);
		return iDays;
    }
	
	/**
	 * 获取两个日期之间的月数
	 * @param beginDate
	 * @param endDate
	 * @return
	 * @throws ParseException 
	 * @throws ParseException
	 * 
	 */
	public static int getMonths(String beginDate1,String endDate1) throws ParseException{
		Date beginDate=getDate(beginDate1);
		Date endDate=getDate(endDate1);
		Calendar former = Calendar.getInstance();
		Calendar latter = Calendar.getInstance();
        former.clear();latter.clear();
        boolean positive = true;
        if(beginDate.after(endDate)){
	        former.setTime(endDate);
	        latter.setTime(beginDate);
	        positive = false;
        }
        else{
        	former.setTime(beginDate);
	        latter.setTime(endDate);
        }
        
        int monthCounter = 0;
        while(former.get(Calendar.YEAR) != latter.get(Calendar.YEAR) ||
        		former.get(Calendar.MONTH) != latter.get(Calendar.MONTH)){
        	former.add(Calendar.MONTH, 1);
        	monthCounter++;
        }
        
                
        if( positive)  return monthCounter;
        else return -monthCounter;
	}
	
	/**获取两个日期之间的月数，向上取整
	 * @param beginDate
	 * @param endDate
	 * @return
	 * @throws ParseException 
	 * @throws ParseException
	 * 
	 */
	public static int getUpMonths(String beginDate1,String endDate1) throws Exception{
		
		/** modify by jliu-2014-03-27 修复闰年 02/29 多算一个月问题 */
		return getUpMonths_Between(beginDate1,endDate1);
/*		
 		Date beginDate=getDate(beginDate1);
		Date endDate=getDate(endDate1);
		Calendar former = Calendar.getInstance();
		Calendar latter = Calendar.getInstance();
        former.clear();latter.clear();
        boolean positive = true;
        if(beginDate.after(endDate)){
	        former.setTime(endDate);
	        latter.setTime(beginDate);
	        positive = false;
        }
        else{
        	former.setTime(beginDate);
	        latter.setTime(endDate);
        }
        
        int monthCounter = 0;
        while(former.get(Calendar.YEAR) != latter.get(Calendar.YEAR) ||
        		former.get(Calendar.MONTH) != latter.get(Calendar.MONTH)){
        	former.add(Calendar.MONTH, 1);
        	monthCounter++;
        }
        
        //modify by xjzhao 当天数前者小于后者时才加一，防止少计算一个月
        if(positive && beginDate1.substring(8).compareTo(endDate1.substring(8)) < 0 ){
        	monthCounter++;
        }
        else if(!positive && beginDate1.substring(8).compareTo(endDate1.substring(8)) > 0 )
        {
        	monthCounter++;
        }
        
        if( positive)
            return monthCounter;
        else
            return -monthCounter;
*/
	}
	
	/**获取两个日期之间的月数，向上取整
	 * @param beginDate
	 * @param endDate
	 * @return
	 * @throws ParseException 
	 * @throws ParseException
	 * 
	 */
	 public static int getUpMonths_Between(String date1,String date2) throws Exception{
	      String beginDate = "";
	      String endDate = "";
	      int months_Between = 0;
	      if(date1.equals(date2)){
	        return months_Between;
	      }
	      else if(date1.compareTo(date2)>0){
	        endDate = date1;
	        beginDate = date2;
	      }
	      else{
	        beginDate = date1;
	        endDate = date2;
	      }
	      Calendar beginCalendar = Calendar.getInstance();
	      beginCalendar.setTime(getDate(beginDate));
	      Calendar endCalendar = Calendar.getInstance();
	      endCalendar.setTime(getDate(endDate));
	      while(beginCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR) 
	           || beginCalendar.get(Calendar.MONTH) != endCalendar.get(Calendar.MONTH))
	      {
	        beginCalendar.add(Calendar.MONTH, 1);
	        months_Between++;
	      }
	      if(!monthEnd(beginDate)){
	        String beginDays = beginDate.substring(8);
	        String endDays = endDate.substring(8);
	        if(endDays.compareTo(beginDays) > 0){
	           months_Between ++;
	        }
	      }
	      return months_Between;
	   }
	 
	/**
	 * 获取两个日期之间的年数  add by xjzhao 2011/04/06
	 * @param beginDate
	 * @param endDate
	 * @return
	 * @throws ParseException 
	 * @throws ParseException
	 * 
	 */
	public static int getYears(String beginDate1,String endDate1) throws ParseException{
		Date beginDate=getDate(beginDate1);
		Date endDate=getDate(endDate1);
		Calendar former = Calendar.getInstance();
		Calendar latter = Calendar.getInstance();
        former.clear();latter.clear();
        boolean positive = true;
        if(beginDate.after(endDate)){
	        former.setTime(endDate);
	        latter.setTime(beginDate);
	        positive = false;
        }
        else{
        	former.setTime(beginDate);
	        latter.setTime(endDate);
        }
        
        int monthCounter = 0;
        while(former.get(Calendar.YEAR) != latter.get(Calendar.YEAR)){
        	former.add(Calendar.YEAR, 1);
        	monthCounter++;
        }
                
        if( positive)  return monthCounter;
        else return -monthCounter;
	}
	
	
	/**获取两个日期之间的年数，向上取整 add by xjzhao 2011/04/06
	 * @param beginDate
	 * @param endDate
	 * @return
	 * @throws ParseException 
	 * @throws ParseException
	 * 
	 */
	public static int getUpYears(String beginDate1,String endDate1) throws ParseException{
		Date beginDate=getDate(beginDate1);
		Date endDate=getDate(endDate1);
		Calendar former = Calendar.getInstance();
		Calendar latter = Calendar.getInstance();
        former.clear();latter.clear();
        boolean positive = true;
        if(beginDate.after(endDate)){
	        former.setTime(endDate);
	        latter.setTime(beginDate);
	        positive = false;
        }
        else{
        	former.setTime(beginDate);
	        latter.setTime(endDate);
        }
        
        int monthCounter = 0;
        while(former.get(Calendar.YEAR) != latter.get(Calendar.YEAR)){
        	former.add(Calendar.YEAR, 1);
        	monthCounter++;
        }
        
        //modify by xjzhao 当天数前者小于后者时才加一，防止少计算一年
        if(positive && beginDate1.substring(5).compareTo(endDate1.substring(5)) < 0 ){
        	monthCounter++;
        }
        else if(!positive && beginDate1.substring(5).compareTo(endDate1.substring(5)) > 0 )
        {
        	monthCounter++;
        }
        
        if( positive)
            return monthCounter;
        else
            return -monthCounter;
	}
	
	public static int compareDate(String date1,String date2) throws ParseException{
		Date date1d=DateUtils.getDate(date1);
		Date date2d=DateUtils.getDate(date2);
		if(date1d.after(date2d)) return -1;
		if(date1d.before(date2d)) return 1;
		else return 0;
	}
	
	
	/**
	 * 功能：转换ESB请求日期格式 19990101 ->1999/01/01
	 * @param date   传入日期
	 * @return   String
	 */
	public static String turnEsbDate(String date)
	{
		if(date==null || "".equals(date))return "";
		if(date.length() == 8) date = date.substring(0, 4)+"/"+date.substring(4, 6)+"/"+date.substring(6, 8);
	    return date;	
	}
	
	 /**
     * 按照指定格式返回当前时间，如果格式为空，则默认为yyyy-MM-dd
     *
     * @param dateFormat
     * @return
     */
    public static Date stringFormatToDate(String date, String dateFormat) {
        if (date == null) {
            return null;
        }

        if (StringUtils.isBlank(dateFormat)) {
            if (date.length() > 10) {
                dateFormat = DEFUALT_LONG_TIME_FORMAT;
            } else {
                dateFormat = DEFUALT_SHOT_TIME_FORMAT;
            }
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            return sdf.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	
	/*public static void main(String[] args) {
	    Calendar calendar= Calendar.getInstance();
	    calendar.add(Calendar.DAY_OF_MONTH, -5);
        int days=getDays( getDateTime(calendar.getTime(), "yyyy-MM-dd"),getDateTime(new Date(), "yyyy-MM-dd"));
        System.out.println(days);
	    
	    //System.out.println(getDateTime(stringFormatToDate("2017-05-10", "yyyyMMdd"),"yyyyMMdd"));;
	    
	    System.out.println( DateUtils.stringFormatToDate("20170515 23:34:12", "yyyyMMdd"));
    }*/
}
