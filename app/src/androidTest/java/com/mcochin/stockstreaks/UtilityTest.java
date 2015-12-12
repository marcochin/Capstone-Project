package com.mcochin.stockstreaks;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.mcochin.stockstreaks.utils.Utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Test for some Utility Methods
 */
public class UtilityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public UtilityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testDuringTradingHours(){
        //Test 9:29am
        Calendar testTime1 = Utility.getNewYorkCalendarInstance();
        testTime1.set(Calendar.HOUR_OF_DAY, 9);
        testTime1.set(Calendar.MINUTE, 29);
        testTime1.set(Calendar.MILLISECOND, 0);

        assertEquals(false, isDuringTradingHours(testTime1));

        //Test 9:30am
        testTime1.set(Calendar.HOUR_OF_DAY, 9);
        testTime1.set(Calendar.MINUTE, 30);

        assertEquals(true, isDuringTradingHours(testTime1));

        //Test 12:00pm
        testTime1.set(Calendar.HOUR_OF_DAY, 12);
        testTime1.set(Calendar.MINUTE, 0);

        assertEquals(true, isDuringTradingHours(testTime1));

        //Test 4:30pm
        testTime1.set(Calendar.HOUR_OF_DAY, 16);
        testTime1.set(Calendar.MINUTE, 30);

        assertEquals(false, isDuringTradingHours(testTime1));

        //Test 12:00am
        testTime1.set(Calendar.HOUR_OF_DAY, 0);
        testTime1.set(Calendar.MINUTE, 0);

        assertEquals(false, isDuringTradingHours(testTime1));

    }

    private boolean isDuringTradingHours(Calendar testTime){
        //9:30am
        Calendar stockMarketOpen = Utility.getCalendarQuickSetup(
                        Utility.STOCK_MARKET_OPEN_HOUR,
                        Utility.STOCK_MARKET_OPEN_MINUTE,
                        0);

        //4:30pm
        Calendar stockMarketClose = Utility.getCalendarQuickSetup(
                        Utility.STOCK_MARKET_UPDATE_HOUR,
                        Utility.STOCK_MARKET_UPDATE_MINUTE,
                        0);

        // If nowTime is between 9:30am EST and 4:30 pm EST
        // assume it is trading hours
        if(!testTime.before(stockMarketOpen) && testTime.before(stockMarketClose)){
            return true;
        }
        return false;
    }

    public void testCanUpdateList(){
        // We updated fri 12/4/2015 @ 4:29pm
        Calendar testUpdateDateTime1 =
                Utility.getCalendarQuickSetup(16, 29, 0, Calendar.DECEMBER, 4, 2015);
        // now sun 12/6/2015 @ 4:20pm
        Calendar testNowTime1 =
                Utility.getCalendarQuickSetup(16, 29, 0, Calendar.DECEMBER, 6, 2015);
        assertEquals(true, canUpdateList(testUpdateDateTime1, testNowTime1));

        // We updated fri 12/4/2015 @ 4:30pm
        Calendar testUpdateDateTime2 =
                Utility.getCalendarQuickSetup(16, 30, 0, Calendar.DECEMBER, 4, 2015);
        // now sun 12/6/2015 @ 4:30pm
        Calendar testNowTime2 =
                Utility. getCalendarQuickSetup(16, 30, 0, Calendar.DECEMBER, 6, 2015);
        assertEquals(false, canUpdateList(testUpdateDateTime2, testNowTime2));

        // We updated th 12/3/2015 @ 4:30pm
        Calendar testUpdateDateTime3 =
                Utility.getCalendarQuickSetup(16, 30, 0, Calendar.DECEMBER, 3, 2015);
        // now th 12/3/2015 @ 4:30pm
        Calendar testNowTime3 =
                Utility. getCalendarQuickSetup(16, 30, 0, Calendar.DECEMBER, 3, 2015);
        assertEquals(false, canUpdateList(testUpdateDateTime3, testNowTime3));

        // We updated wed 12/2/2015 @ 4:29pm
        Calendar testUpdateDateTime4 =
                Utility.getCalendarQuickSetup(16, 29, 0, Calendar.DECEMBER, 2, 2015);
        // now th 12/3/2015 @ 4:29pm
        Calendar testNowTime4 =
                Utility. getCalendarQuickSetup(16, 29, 0, Calendar.DECEMBER, 3, 2015);
        assertEquals(true, canUpdateList(testUpdateDateTime4, testNowTime4));

        // We updated wed 12/2/2015 @ 4:30pm
        Calendar testUpdateDateTime5 =
                Utility.getCalendarQuickSetup(16, 30, 0, Calendar.DECEMBER, 2, 2015);
        // now th 12/3/2015 @ 4:29pm
        Calendar testNowTime5 =
                Utility. getCalendarQuickSetup(16, 29, 0, Calendar.DECEMBER, 3, 2015);
        assertEquals(false, canUpdateList(testUpdateDateTime5, testNowTime5));
    }

    /**
     * Checks to see if the stock list is up to date, if not then update
     * @param testUpdateDateTime the test update time that should be in the db.
     * @param testNowTime The "now" time to test against testUpdateDateTime.
     * @return true if list can be updated, else false
     */
    private static boolean canUpdateList(Calendar testUpdateDateTime, Calendar testNowTime){
        Calendar fourThirtyTime = Calendar.getInstance();
        fourThirtyTime.setTimeInMillis(testNowTime.getTimeInMillis());

        fourThirtyTime.set(Calendar.HOUR_OF_DAY, Utility.STOCK_MARKET_UPDATE_HOUR);
        fourThirtyTime.set(Calendar.MINUTE, Utility.STOCK_MARKET_UPDATE_MINUTE);
        fourThirtyTime.set(Calendar.MILLISECOND, 0);

        int dayOfWeek = testNowTime.get(Calendar.DAY_OF_WEEK);

        // ALGORITHM:
        // If nowTime is sunday or saturday
        // check if lastUpdateTime was before LAST FRIDAY @ 4:30pm EST, if so update.
        // If nowTime is monday < 4:30pm EST,
        // check if lastUpdateTime was before LAST LAST FRIDAY @ 4:30pm EST, if so update.
        // If nowTime(not monday) < 4:30pm EST,
        // check if lastUpdateTime was before YESTERDAY @ 4:30pm EST, if so update.
        // If nowTime >= 4:30pm EST,
        // check if lastUpdateTime was before TODAY @ 4:30pmEST, if so update.
        if ((dayOfWeek == Calendar.SATURDAY)) {
            // 1 days ago from Saturday is last Friday @ 4:30pm EST
            fourThirtyTime.add(Calendar.DAY_OF_MONTH, -1);

        } else if ((dayOfWeek == Calendar.SUNDAY)) {
            // 2 days ago from Sunday is last Friday @ 4:30pm EST
            fourThirtyTime.add(Calendar.DAY_OF_MONTH, -2);

        } else if(testNowTime.before(fourThirtyTime)) {
            if (dayOfWeek == Calendar.MONDAY) {
                // 3 days ago from Monday is last Friday @ 4:30pm EST
                fourThirtyTime.add(Calendar.DAY_OF_MONTH, -3);
            } else{
                // 1 day ago is yesterday @ 4:30pm EST
                fourThirtyTime.add(Calendar.DAY_OF_MONTH, -1);
            }
        }

//        Log.d("test", "testUpdateTime Month: " + testUpdateDateTime.get(Calendar.MONTH)
//                + " Day: " + testUpdateDateTime.get(Calendar.DAY_OF_MONTH)
//                + " Year: " + testUpdateDateTime.get(Calendar.YEAR)
//                + " Hour: " + testUpdateDateTime.get(Calendar.HOUR_OF_DAY)
//                + " Minute: " + testUpdateDateTime.get(Calendar.MINUTE)
//                + " Day of Week: " + testUpdateDateTime.get(Calendar.DAY_OF_WEEK));
//
//        Log.d("test", "Four30 Month: " + fourThirtyTime.get(Calendar.MONTH)
//                + " Day: " + fourThirtyTime.get(Calendar.DAY_OF_MONTH)
//                + " Year: " + fourThirtyTime.get(Calendar.YEAR)
//                + " Hour: " + fourThirtyTime.get(Calendar.HOUR_OF_DAY)
//                + " Minute: " + fourThirtyTime.get(Calendar.MINUTE)
//                + " Day of Week: " + fourThirtyTime.get(Calendar.DAY_OF_WEEK));

        //if lastUpdateTime is before the recentClose time, then update.
        if(testUpdateDateTime.before(fourThirtyTime)){
            return true;
        }
        return false;
    }

    public void testCalendarTimeInMilli() {
        Calendar newYork = Utility.getNewYorkCalendarInstance();
        Calendar myTime = Calendar.getInstance();
        myTime.set(Calendar.HOUR, 12);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd\n@ h:mm a", Locale.US);

        Log.d("Time", sdf.format(myTime.getTime()));
//        Log.d("Time", "TimeInMilli: newYOrk " + newYork.getTimeInMillis() + " " + newYork.get(Calendar.HOUR_OF_DAY));
//        Log.d("Time", "TimeInMilli: myTime " + myTime.getTimeInMillis()+ " " + myTime.get(Calendar.HOUR_OF_DAY));

    }
}
