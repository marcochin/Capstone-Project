# Capstone-Project
Android: Stock Streaks is an app that follows and tracks historical data of your favorite stocks. It calculates how many times a stock has gone up or down in a row and displays the information to you.

---
### Notes
This app has incorporated `Google Analytics` and `Google AdMob` libraries, but with their service ids removed.
Please insert your own ids as shown below:

For Analytics in `res/xml/analytics_tracker.xml`:

    <string name="ga_trackingId">YOUR_ANALYTICS_TRACKING_ID_HERE</string>
    
For Admob: in `res/strings.xml`:

    // This current id is a interstitial TEST id. Replace it with your own personal id.
    <string name="interstitial_ad_unit_id">ca-app-pub-3940256099942544/1033173712</string>
