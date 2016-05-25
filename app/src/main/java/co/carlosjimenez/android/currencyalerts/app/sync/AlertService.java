/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Carlos Andres Jimenez <apps@carlosandresjimenez.co>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package co.carlosjimenez.android.currencyalerts.app.sync;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.v7.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;

import co.carlosjimenez.android.currencyalerts.app.DetailActivity;
import co.carlosjimenez.android.currencyalerts.app.R;
import co.carlosjimenez.android.currencyalerts.app.Utility;
import co.carlosjimenez.android.currencyalerts.app.data.Alert;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 * This service recalculates the average of the rate selected for alerts and
 * also notifies the user is the fluctuation is above the threshold.
 */
public class AlertService extends IntentService {

    private final static String LOG_TAG = AlertService.class.getSimpleName();

    private static final String EXTRA_CURRENT_RATE = "co.carlosjimenez.android.currencyalerts.app.sync.extra.CURRENT_RATE";

    // An ID used to post the notification.
    public static final int NOTIFICATION_ID = 1;

    // These indices are tied to FOREX_COLUMNS.  If FOREX_COLUMNS changes, these
    // must change.
    static final int COL_RATE_DATE = 13;
    static final int COL_RATE_VAL = 14;

    private Alert mAlert;

    public AlertService() {
        super("AlertService");
    }

    /**
     * Starts this service to perform action DataUpdated with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @param context A Context of the application package implementing this class.
     * @param currentRate The current rate for the requested alert
     *
     * @see IntentService
     */
    public static void startAlertService(Context context, double currentRate) {
        Intent intent = new Intent(context, AlertService.class);
        intent.setAction(ForexSyncAdapter.ACTION_DATA_UPDATED);
        intent.putExtra(EXTRA_CURRENT_RATE, currentRate);
        context.startService(intent);
    }

    /**
     * Hook method called when a component calls startService() with the proper
     * intent. This method serves as the Executor in the Command Processor
     * Pattern. It receives an Intent, which serves as the Command, and executes
     * some action based on that intent in the context of this service.
     *
     * @param intent Intent that contains the current rate for the requested alert
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final double currentRate = intent.getDoubleExtra(EXTRA_CURRENT_RATE, -1);
            handleActionDataUpdated(currentRate);
        }
    }

    /**
     * Handle action DataUpdated in the provided background thread with the provided
     * parameters.
     *
     * @param currentRate Current rate for the requested alert
     */
    private void handleActionDataUpdated(double currentRate) {
        if (currentRate <= 0) {
            return;
        }

        Context context = getApplicationContext();
        mAlert = Utility.getAlertSettings(context, false);
        mAlert.setCurrentRate(currentRate);

        if (!mAlert.sendNotifications() || !mAlert.isEnabled()) {
            return;
        }

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        long syncDate = Utility.getForexSyncDate(context);

        if (julianDate != syncDate) {
            recalculateAverage();
        }

        validateRateFluctuation();

    }

    /**
     * Recalculates the average for the selected rate within the specified range of days.
     */
    private void recalculateAverage() {
        String[] currencies = {mAlert.getCurrencyFrom().getId(), mAlert.getCurrencyTo().getId()};
        Uri rateUri = ForexContract.RateEntry.buildCurrencyRateWithValue(currencies, 1);

        Cursor data = null;
        try {
            data = getContentResolver().query(rateUri,
                    ForexContract.RATE_CURRENCY_COLUMNS,
                    null,
                    null,
                    ForexContract.RateEntry.COLUMN_RATE_DATE + " DESC");

            if (data == null) {
                Log.d(LOG_TAG, "Alert Service Query Finished: No data returned");

                return;
            }
            if (!data.moveToFirst()) {
                Log.d(LOG_TAG, "Alert Service Query Finished: No data returned");

                data.close();
                return;
            }

            long lDate = 0;
            int i = 0;
            double dRateAverage = 0;
            double dVal = 0;

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();
            long lMinDate = dayTime.setJulianDay(julianDate - mAlert.getPeriod());

            for (i = 0; i < data.getCount() && i < mAlert.getPeriod(); i++) {
                data.moveToPosition(i);

                lDate = data.getLong(COL_RATE_DATE);
                if (lDate < lMinDate) {
                    break;
                }

                dVal = data.getDouble(COL_RATE_VAL);
                dRateAverage += dVal;
            }

            dRateAverage = dRateAverage / i;
            mAlert.setRateAverage(dRateAverage);
            setRateAverage(this, String.valueOf(dRateAverage));
        } finally {
            if (data != null && !data.isClosed()) {
                data.close();
            }
        }

    }

    /**
     * Validates the fluctuation and check if the notifications is needed.
     */
    private void validateRateFluctuation() {

        // Validate rate percentage increase against the one on the shared preferences
        if (mAlert.sendNotifications() &&
                mAlert.getCurrentFluctuation() > mAlert.getFluctuation())
        {
            notifyUser();
        }
    }

    /**
     * Send a notification to the user if the rate is below or above the specified threshold.
     */
    private void notifyUser() {
        // Send Alert to the user about increase on the rate if the notification setting is activated
        Context context = getApplicationContext();
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, DetailActivity.class);
        String[] currencies = {mAlert.getCurrencyFrom().getId(), mAlert.getCurrencyTo().getId()};
        Uri rateUri = ForexContract.RateEntry.buildCurrencyRateWithValue(currencies, 1);
        intent.setData(rateUri);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        int color;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            color = getResourceColorV22(R.color.colorAccentDark);
        else
            color = getResourceColor(R.color.colorAccentDark);

        String notificationText = getString(R.string.notification_text,
                mAlert.getCurrencyFrom().getId(),
                mAlert.getCurrencyTo().getId(),
                mAlert.isPositiveFluctuation() ? getString(R.string.notification_positive_fluctuation) : getString(R.string.notification_negative_fluctuation),
                mAlert.getCurrentFluctuation());

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setSmallIcon(mAlert.isPositiveFluctuation() ? R.drawable.ic_trending_up_white_24dp : R.drawable.ic_trending_down_white_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setColor(color)
                        .setContentTitle(context.getString(R.string.notification_title))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notificationText))
                        .setContentText(notificationText);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * Sets the rate average into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c              Context to get the PreferenceManager from.
     * @param average        The rate average for the requested alert
     */
    private void setRateAverage(Context c, String average) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(c.getString(R.string.pref_alert_check_rate_average_key), average);
        spe.commit();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public int getResourceColor(@ColorRes int resId) {
        return getApplicationContext().getColor(resId);
    }

    @SuppressWarnings("deprecation")
    public int getResourceColorV22(@ColorRes int resId) {
        return getApplicationContext().getResources().getColor(resId);
    }
}
