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


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import co.carlosjimenez.android.currencyalerts.app.data.Alert;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_sync_frequency_key)));

        initDeleteAlertPreference();
    }

    // Registers a shared preference change listener that gets notified when preferences change
    @Override
    protected void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    // Unregisters a shared preference change listener
    @Override
    protected void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Set the preference summaries
        setPreferenceSummary(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();
        String key = preference.getKey();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            preference.setSummary(stringValue);
        }

    }

    private void initDeleteAlertPreference() {
        Preference deleteAlertsPref = findPreference(getString(R.string.pref_delete_alerts_key));
        deleteAlertsPref.setOnPreferenceClickListener(this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        setAlertSummary(deleteAlertsPref);
    }

    private void setAlertSummary(Preference preference) {
        Alert alert = Utility.getAlertSettings(this);

        if (alert == null || alert.getCurrencyFrom() == null || alert.getCurrencyTo() == null) {
            return;
        }

        if (alert.isEnabled()) {
            preference.setSummary(getString(R.string.format_alert_summary,
                    alert.getCurrencyFrom().getId(),
                    alert.getCurrencyTo().getId(),
                    alert.getPeriod(),
                    alert.getFluctuation()));
        } else {
            preference.setSummary(getString(R.string.no_alert_summary));
        }
    }

    // This gets called before the preference is changed
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals(getString(R.string.pref_delete_alerts_key))) {
                new DeleteAlertsTask(this).execute();
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_alert_check_enabled_key)) ||
                key.equals(getString(R.string.pref_alert_check_currency_from_key)) ||
                key.equals(getString(R.string.pref_alert_check_currency_to_key)) ||
                key.equals(getString(R.string.pref_alert_check_period_key)) ||
                key.equals(getString(R.string.pref_alert_check_fluctuation_key))) {
            setAlertSummary(findPreference(getString(R.string.pref_delete_alerts_key)));
        }
    }

    public class DeleteAlertsTask extends AsyncTask<Void, Void, Integer> {

        private final Context mContext;

        public DeleteAlertsTask(Context context) {
            mContext = context;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            deleteAlert(mContext);
            return 0;
        }
    }

    static private void deleteAlert(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.remove(c.getString(R.string.pref_alert_check_enabled_key));
        spe.remove(c.getString(R.string.pref_alert_check_currency_from_key));
        spe.remove(c.getString(R.string.pref_alert_check_currency_to_key));
        spe.remove(c.getString(R.string.pref_alert_check_period_key));
        spe.remove(c.getString(R.string.pref_alert_check_fluctuation_key));
        spe.commit();
    }
}
