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

package co.carlosjimenez.android.currencyalerts.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.text.SimpleDateFormat;

import co.carlosjimenez.android.currencyalerts.app.data.Alert;
import co.carlosjimenez.android.currencyalerts.app.data.Currency;
import co.carlosjimenez.android.currencyalerts.app.sync.ForexSyncAdapter;
import co.carlosjimenez.android.currencyalerts.app.sync.LoadCurrencyTask;

public class Utility {

    private static final String LOG_TAG = Utility.class.getSimpleName();

    public static final String DATE_FORMAT = "yyyy/MM/dd";
    public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

    /**
     * Helper method to obtain the Currency Sync Status from the Shared Preferences
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the currency sync status, integer type
     */
    @SuppressWarnings("ResourceType")
    static public
    @LoadCurrencyTask.CurrencyStatus
    int getCurrencyStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_currency_status_key), LoadCurrencyTask.CURRENCY_STATUS_UNKNOWN);
    }

    /**
     * Helper method to obtain the Rates Sync Status from the Shared Preferences
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the rates sync status, integer type
     */
    @SuppressWarnings("ResourceType")
    static public
    @ForexSyncAdapter.ForexStatus
    int getForexStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_forex_status_key), ForexSyncAdapter.FOREX_STATUS_UNKNOWN);
    }

    /**
     * Helper method to check on the Shared Preferences if the alerts are enabled
     *
     * @param c         Context used to get the SharedPreferences
     * @return          true if the alert are enabled
     */
    static public boolean isAlertEnabled(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getBoolean(c.getString(R.string.pref_alert_check_enabled_key),
                Boolean.valueOf(c.getString(R.string.pref_alert_check_enabled_default)));
    }

    /**
     * Helper method to obtain the Alert Settings from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @param defaults  if true, it will only return the default values on Shared Preferences
     * @return          the alert settings, Alert type
     */
    static public Alert getAlertSettings(Context c, boolean defaults) {
        boolean enabled;
        int alarmCheckPeriod;
        float alarmCheckFluctuation;
        boolean sendNotifications;
        double averageRate;

        Currency currencyFrom = new Currency();
        Currency currencyTo = new Currency();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);

        if (defaults) {

            enabled = Boolean.valueOf(c.getString(R.string.pref_alert_check_enabled_default));
            currencyFrom.setId("");
            currencyTo.setId("");
            alarmCheckPeriod = Integer.parseInt(c.getString(R.string.pref_alert_check_period_default));
            alarmCheckFluctuation = Float.parseFloat(c.getString(R.string.pref_alert_check_fluctuation_default));
            sendNotifications = false;
            averageRate = -1;

        } else {

            enabled = sp.getBoolean(c.getString(R.string.pref_alert_check_enabled_key),
                    Boolean.valueOf(c.getString(R.string.pref_alert_check_enabled_default)));

            currencyFrom.setId(sp.getString(c.getString(R.string.pref_alert_check_currency_from_key),
                    ""));

            currencyTo.setId(sp.getString(c.getString(R.string.pref_alert_check_currency_to_key),
                    ""));

            alarmCheckPeriod = sp.getInt(c.getString(R.string.pref_alert_check_period_key),
                    Integer.parseInt(c.getString(R.string.pref_alert_check_period_default)));

            alarmCheckFluctuation = sp.getFloat(c.getString(R.string.pref_alert_check_fluctuation_key),
                    Float.parseFloat(c.getString(R.string.pref_alert_check_fluctuation_default)));

            sendNotifications = sp.getBoolean(c.getString(R.string.pref_enable_notifications_key),
                    Boolean.valueOf(c.getString(R.string.pref_enable_notifications_default)));

            averageRate = Double.parseDouble(sp.getString(c.getString(R.string.pref_alert_check_rate_average_key),
                    c.getString(R.string.pref_alert_check_rate_average_default)));
        }

        return new Alert(enabled, currencyFrom, currencyTo, alarmCheckPeriod, alarmCheckFluctuation, false, sendNotifications, averageRate);
    }

    /**
     * Helper method to obtain the Rate Average from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the rate average, double type
     */
    static public double getRateAverage(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return Double.parseDouble(sp.getString(c.getString(R.string.pref_alert_check_rate_average_key),
                c.getString(R.string.pref_alert_check_rate_average_default)));
    }

    /**
     * Helper method to obtain the Sync Frequency from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the forex sync frequency, integer type
     */
    static public int getSyncFrequency(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_sync_frequency_default), Integer.parseInt(c.getString(R.string.pref_sync_frequency_default)));
    }

    /**
     * Helper method to obtain the Forex Sync Date from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the forex sync date, long type
     */
    static public long getForexSyncDate(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        long syncTime = sp.getLong(c.getString(R.string.pref_forex_sync_date_key), 0);

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianDate = Time.getJulianDay(syncTime, dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();
        return dayTime.setJulianDay(julianDate);
    }

    /**
     * Helper method to obtain the Forex Sync Datetime from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the forex sync datetime, long type
     */
    static public long getForexSyncDateTime(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getLong(c.getString(R.string.pref_forex_sync_date_key), 0);
    }

    /**
     * Helper method to obtain the currencies to be synced from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the currencies to be synced, String type
     */
    static public String getSyncCurrencies(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getString(c.getString(R.string.pref_displayed_currencies_key), "");
    }

    /**
     * Helper method to obtain the main currency from the Shared Preferences.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          the main currency, Currency type
     */
    static public Currency getMainCurrency(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return new Currency(
                sp.getString(c.getString(R.string.pref_main_currency_id_key), c.getString(R.string.pref_cu_id_us)),
                sp.getString(c.getString(R.string.pref_main_currency_name_key), c.getString(R.string.pref_cu_name_us)),
                sp.getString(c.getString(R.string.pref_main_currency_symbol_key), c.getString(R.string.pref_cu_symbol_us)),
                sp.getString(c.getString(R.string.pref_main_country_code_key), c.getString(R.string.pref_co_code_us)),
                sp.getString(c.getString(R.string.pref_main_country_name_key), c.getString(R.string.pref_co_name_us)),
                sp.getString(c.getString(R.string.pref_main_country_flag_key), c.getString(R.string.pref_co_flag_us)));
    }

    /**
     * Helper method to check on the Shared Preferences if notifications are enabled.
     *
     * @param c         Context used to get the SharedPreferences
     * @return          true if notifications are enabled
     */
    static public boolean isNotificationsEnabled(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getBoolean(c.getString(R.string.pref_enable_notifications_key),
                Boolean.valueOf(c.getString(R.string.pref_enable_notifications_default)));
    }

    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c         Context used to get the ConnectivityManager
     * @return          true if the network is available
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Helper method to convert a date into something to display to users.  As classy
     * and polished a user experience as "2014/01/02" is, we can do better.
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getDateString(Context context, long dateInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        return dateFormat.format(dateInMillis);
    }

    /**
     * Helper method to convert a date into something to display to users.  As classy and
     * polished a user experience as "2014/01/02 12:00:00" is, we can do better.
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getDateTimeString(Context context, long dateInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Utility.DATETIME_FORMAT);
        return dateFormat.format(dateInMillis);
    }

    /**
     * Helper method to format the description for the flag icon.
     *
     * @param context       Context to use for resource localization
     * @param country_name  Name of the country to be formatted
     * @return              A user-friendly representation of the country name.
     */
    public static String formatCountryFlagName(Context context, String country_name) {
        return context.getString(R.string.format_country_flag_description, country_name);
    }

    /**
     * Helper method to format the currency rate.
     *
     * @param context       Context to use for resource localization
     * @param currency_symbol  Currency symbol of the currency
     * @param currencyRate  Currency value to be formatted
     * @return              A user-friendly representation of the currency.
     */
    public static String formatCurrencyRate(Context context, String currency_symbol, double currencyRate) {

        String rate = context.getString(R.string.format_currency_rate, currency_symbol, currencyRate);
        String formattedRate = "";

        int prefixLen = currency_symbol.length();
        int j = 0;

        if (rate.startsWith(currency_symbol + " "))
            prefixLen++;

        for (int i = rate.length() - 1; i >= prefixLen; i--) {
            if (i <= rate.length() - 5) {
                if (j < 2) {
                    j++;
                } else {
                    formattedRate = "," + formattedRate;
                    j = 0;
                }
            }
            formattedRate = rate.charAt(i) + formattedRate;
        }

        formattedRate = rate.substring(0, prefixLen) + formattedRate;

        return formattedRate;
    }

    /**
     * Helper method to calculate the font size of the rate according the the length
     *
     * @param length       Length of the string to be displayed
     * @return             The appropriate size of the font size in pixels.
     */
    public static int getRateFontPXSize(int length) {
        if (length <= 10)
            return 80;
        else if (length == 11)
            return 72;
        else if (length == 12)
            return 66;
        else if (length == 13)
            return 63;
        else if (length == 14)
            return 58;
        else if (length == 15)
            return 53;
        else if (length == 16)
            return 49;
        else if (length == 17)
            return 45;
        else if (length == 18)
            return 42;
        else if (length == 19)
            return 41;
        else if (length == 20)
            return 40;
        else if (length == 21)
            return 37;
        else if (length == 22)
            return 34;
        else if (length == 23)
            return 33;
        else if (length == 24)
            return 33;
        else if (length == 25)
            return 31;
        else if (length == 26)
            return 29;
        else if (length == 27)
            return 28;
        else if (length == 28)
            return 27;
        else if (length == 29)
            return 27;
        else if (length == 30)
            return 25;
        else if (length == 31)
            return 25;
        else if (length == 32)
            return 24;
        else if (length == 33)
            return 24;
        else if (length == 34)
            return 23;
        else if (length >= 35)
            return 23;

        return -1;
    }
}
