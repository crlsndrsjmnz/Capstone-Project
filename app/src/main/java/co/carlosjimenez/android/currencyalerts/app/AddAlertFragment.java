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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import co.carlosjimenez.android.currencyalerts.app.data.Alert;
import co.carlosjimenez.android.currencyalerts.app.data.Currency;

/**
 * Fragment that appears as a dialog and shows the options to set an alert for the
 * specified currencies.
 *
 * <p/>Created by carlosjimenez on 5/23/16.
 */
public class AddAlertFragment extends DialogFragment {

    private static final String LOG_TAG = AddAlertFragment.class.getSimpleName();

    @BindView(R.id.alarm_period)
    Spinner mSpPeriod;
    @BindView(R.id.alarm_fluctuation)
    TextView mTvFluctuation;

    private String mSelectedPeriod = "";
    private Alert alertValues;

    Currency mCurrencyFrom;
    Currency mCurrencyTo;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_alarm, null);
        ButterKnife.bind(this, dialogView);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mCurrencyFrom = arguments.getParcelable(DetailActivityFragment.CURRENCY_FROM);
            mCurrencyTo = arguments.getParcelable(DetailActivityFragment.CURRENCY_TO);
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.alarm_period_labels, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpPeriod.setAdapter(adapter);

        alertValues = Utility.getAlertSettings(getContext(), false);

        if (alertValues.getCurrencyFrom().equals(mCurrencyFrom) &&
                alertValues.getCurrencyTo().equals(mCurrencyTo)) {
            setSelectedPeriod(String.valueOf(alertValues.getPeriod()));
            mTvFluctuation.setText(String.valueOf(alertValues.getFluctuation()));
        } else {
            alertValues = Utility.getAlertSettings(getContext(), true);
            alertValues.clearAverage();
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        alertValues.setEnabled(true);
                        alertValues.setCurrencyFrom(mCurrencyFrom);
                        alertValues.setCurrencyTo(mCurrencyTo);
                        alertValues.setPeriod(Integer.parseInt(mSelectedPeriod));
                        alertValues.setFluctuation(Float.parseFloat(mTvFluctuation.getText().toString()));

                        new UpdateAlarmTask(getActivity()).execute(alertValues);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddAlertFragment.this.getDialog().cancel();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    private void setSelectedPeriod(String period) {
        int i;
        String[] periodArray = getResources().getStringArray(R.array.alarm_period_values);
        for (i = 0; i < periodArray.length; i++) {
            if (periodArray[i].equals(period))
                break;
        }
        mSpPeriod.setSelection(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_width);
        int height = getResources().getDimensionPixelSize(R.dimen.dialog_height);
        getDialog().getWindow().setLayout(width, height);
    }

    @OnItemSelected(R.id.alarm_period)
    void onSelectUserType(int position) {
        mSelectedPeriod = getResources().getStringArray(R.array.alarm_period_values)[position];
    }

    /**
     * This class is used to update the alert settings on the shared preferences.
     *
     * <p>Shared preferences should not be updated on the main UI thread as it uses commit to
     * write to the shared preferences.
     *
     * @see AsyncTask
     */
    public class UpdateAlarmTask extends AsyncTask<Alert, Void, Integer> {

        private final Context mContext;

        public UpdateAlarmTask(Context context) {
            mContext = context;
        }

        @Override
        protected Integer doInBackground(Alert... params) {
            setAlertSettings(mContext, params[0]);
            return 0;
        }
    }

    /**
     * Sets the alert settings into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param c     Context to get the PreferenceManager from.
     * @param alert Alert settings to be stored.
     */
    private void setAlertSettings(Context c, Alert alert) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean(c.getString(R.string.pref_alert_check_enabled_key), alert.isEnabled());
        spe.putString(c.getString(R.string.pref_alert_check_currency_from_key), alert.getCurrencyFrom().getId());
        spe.putString(c.getString(R.string.pref_alert_check_currency_to_key), alert.getCurrencyTo().getId());
        spe.putInt(c.getString(R.string.pref_alert_check_period_key), alert.getPeriod());
        spe.putFloat(c.getString(R.string.pref_alert_check_fluctuation_key), alert.getFluctuation());

        if (alert.isClearAverage()) {
            spe.remove(c.getString(R.string.pref_alert_check_rate_average_key));
        }

        spe.commit();
    }

}