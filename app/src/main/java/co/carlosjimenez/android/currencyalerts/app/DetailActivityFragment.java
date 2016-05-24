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

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.carlosjimenez.android.currencyalerts.app.data.Alert;
import co.carlosjimenez.android.currencyalerts.app.data.Currency;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

    static final String DETAIL_URI = "URI";
    static final String CURRENCY_FROM = "CURRENCY_FROM";
    static final String CURRENCY_TO = "CURRENCY_TO";

    // These indices are tied to FOREX_COLUMNS.  If FOREX_COLUMNS changes, these
    // must change.
    static final int COL_RATE_ID = 0;
    static final int COL_CURRENCY_FROM_ID = 1;
    static final int COL_CURRENCY_FROM_NAME = 2;
    static final int COL_CURRENCY_FROM_SYMBOL = 3;
    static final int COL_COUNTRY_FROM_CODE = 4;
    static final int COL_COUNTRY_FROM_NAME = 5;
    static final int COL_COUNTRY_FROM_FLAG = 6;
    static final int COL_CURRENCY_TO_ID = 7;
    static final int COL_CURRENCY_TO_NAME = 8;
    static final int COL_CURRENCY_TO_SYMBOL = 9;
    static final int COL_COUNTRY_TO_CODE = 10;
    static final int COL_COUNTRY_TO_NAME = 11;
    static final int COL_COUNTRY_TO_FLAG = 12;
    static final int COL_RATE_DATE = 13;
    static final int COL_RATE_VAL = 14;

    private static final int DETAIL_LOADER = 0;
    private static final int DEFAULT_DAYS_FOREX_AVERAGE = 30;

    @BindView(R.id.detail_period_textview)
    TextView mTvPeriod;
    @BindView(R.id.detail_max_rate_textview)
    TextView mTvMaxRate;
    @BindView(R.id.detail_min_rate_textview)
    TextView mTvMinRate;
    @BindView(R.id.detail_average_textview)
    TextView mTvAverageRate;
    @BindView(R.id.currencyFromFlag)
    ImageView mIvFlagFrom;
    @BindView(R.id.currencyToFlag)
    ImageView mIvFlagTo;
    @BindView(R.id.currencyFromDescription)
    TextView mTvCurrencyFromDesc;
    @BindView(R.id.currencyToDescription)
    TextView mTvCurrencyToDesc;
    @BindView(R.id.currencyFromRate)
    TextView mTvCurrencyFromRate;
    @BindView(R.id.currencyToRate)
    TextView mTvCurrencyToRate;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.adView)
    AdView mAdView;

    private final Handler mHandler = new Handler();
    private static final int AD_DELAY_MILLISECONDS = 1000;

    private Uri mUri;
    private AppCompatActivity mContext;
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final String FOREX_SHARE_HASHTAG = " #CurrencyRatesApp";

    private String mDisplayedCurrencyIds;
    private String mDisplayedCurrencyNames;
    private String mDisplayedRate;

    private String mCurrencyFromId;
    private String mCurrencyToId;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, rootView);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
        }

        mContext = (AppCompatActivity) getActivity();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(mContext);

        if (mToolbar != null) {
            mContext.setSupportActionBar(mToolbar);
            mContext.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mContext.getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadAd();
            }
        }, AD_DELAY_MILLISECONDS);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivity() instanceof DetailActivity) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            finishCreatingMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_share:
                sendShareHitToAnalytics(mDisplayedCurrencyIds, mDisplayedCurrencyNames);
                break;
            case android.R.id.home:
                mContext.finishAfterTransition();
                return true;
            case R.id.action_alert:
                openAlertDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareForecastIntent());

        menuItem = menu.findItem(R.id.action_alert);
        MenuTint.colorMenuItem(menuItem, getResources().getColor(R.color.detail_toolbar_icon_color), null);
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mDisplayedRate + FOREX_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != mUri) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    ForexContract.RATE_CURRENCY_COLUMNS,
                    null,
                    null,
                    ForexContract.RateEntry.COLUMN_RATE_DATE + " DESC"
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null) {
            Log.d(LOG_TAG, "Detail Forex Loader Finished: No data returned");

            return;
        }
        if (!data.moveToFirst()) {
            Log.d(LOG_TAG, "Detail Forex Loader Finished: No data returned");

            data.close();
            return;
        }

        long lDate = 0;
        String sDate = "";
        String sCurrency = "";
        int i = 0;
        double dRateAverage = 0;
        double dVal = 0;
        double dMinVal = 0;
        double dMaxVal = 0;

        data.moveToPosition(0);

        mCurrencyFromId = data.getString(COL_CURRENCY_FROM_ID);
        String currencyFromName = data.getString(COL_CURRENCY_FROM_NAME);
        String currencyFromSymbol = data.getString(COL_CURRENCY_FROM_SYMBOL);
        String countryFromName = data.getString(COL_COUNTRY_FROM_NAME);
        double currencyFromRate = ForexContract.RateEntry.getRateFromUri(mUri);

        mCurrencyToId = data.getString(COL_CURRENCY_TO_ID);
        String currencyToName = data.getString(COL_CURRENCY_TO_NAME);
        String currencyToSymbol = data.getString(COL_CURRENCY_TO_SYMBOL);
        String countryToName = data.getString(COL_COUNTRY_TO_NAME);
        double currencyToRate = ForexContract.RateEntry.getRateFromUri(mUri) * data.getDouble(COL_RATE_VAL);

        Glide.with(getActivity())
                .load(data.getString(COL_COUNTRY_FROM_FLAG))
                .error(R.drawable.globe)
                .crossFade()
                .into(mIvFlagFrom);
        mIvFlagFrom.setContentDescription(Utility.formatCountryFlagName(mContext, countryFromName));

        Glide.with(getActivity())
                .load(data.getString(COL_COUNTRY_TO_FLAG))
                .error(R.drawable.globe)
                .crossFade()
                .into(mIvFlagTo);
        mIvFlagFrom.setContentDescription(Utility.formatCountryFlagName(mContext, countryToName));

        mTvCurrencyFromDesc.setText(currencyFromName);
        mTvCurrencyFromDesc.setContentDescription(currencyFromName);

        mTvCurrencyFromRate.setText(Utility.formatCurrencyRate(getActivity(), currencyFromSymbol, currencyFromRate));
        mTvCurrencyFromRate.setContentDescription(String.valueOf(currencyFromRate) + " " + currencyFromName);

        mTvCurrencyToDesc.setText(currencyToName);
        mTvCurrencyToDesc.setContentDescription(currencyToName);

        mTvCurrencyToRate.setText(Utility.formatCurrencyRate(getActivity(), currencyToSymbol, currencyToRate));
        mTvCurrencyToRate.setContentDescription(String.valueOf(currencyToRate) + " " + currencyToName);

        Time dayTime = new Time();
        dayTime.setToNow();

        int julianDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        dayTime = new Time();
        long lMinDate = dayTime.setJulianDay(julianDate - DEFAULT_DAYS_FOREX_AVERAGE);

        sDate = Utility.getDateString(getActivity(), data.getLong(COL_RATE_DATE));

        for (i = 0; i < data.getCount() && i < DEFAULT_DAYS_FOREX_AVERAGE; i++) {
            data.moveToPosition(i);

            lDate = data.getLong(COL_RATE_DATE);
            if (lDate < lMinDate) {
                break;
            }

            sCurrency = data.getString(COL_CURRENCY_TO_ID);
            sDate = Utility.getDateString(getActivity(), lDate);
            dVal = data.getDouble(COL_RATE_VAL);
            dRateAverage += dVal;

            if (i == 0) {
                dMinVal = dVal;
                dMaxVal = dVal;
            } else {
                dMinVal = dMinVal < dVal ? dMinVal : dVal;
                dMaxVal = dMaxVal > dVal ? dMaxVal : dVal;
            }
        }
        dRateAverage = dRateAverage / i;

        if (data.getCount() > 1)
            mTvPeriod.setText(data.getCount() + " days");
        else
            mTvPeriod.setText(data.getCount() + " day");
        mTvMaxRate.setContentDescription(mTvPeriod.getText());

        mTvMaxRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), dMaxVal));
        mTvMaxRate.setContentDescription(mTvMaxRate.getText());

        mTvMinRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), dMinVal));
        mTvMinRate.setContentDescription(mTvMinRate.getText());

        mTvAverageRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), dRateAverage));
        mTvAverageRate.setContentDescription(mTvAverageRate.getText());

        // String text to share if user clicks on share menu icon
        mDisplayedRate = String.format("%s - %s %s = %s %s", sDate, currencyFromRate, mCurrencyFromId, currencyToRate, mCurrencyToId);
        mDisplayedCurrencyIds = mCurrencyFromId + "-" + mCurrencyToId;
        mDisplayedCurrencyNames = currencyToName;

        mContext.supportStartPostponedEnterTransition();

        if (null != mToolbar) {
            Menu menu = mToolbar.getMenu();
            if (null != menu) menu.clear();
            mToolbar.inflateMenu(R.menu.detailfragment);
            finishCreatingMenu(mToolbar.getMenu());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void loadAd() {
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder()
                    .build();
            mAdView.loadAd(adRequest);
        }
    }

    private void sendShareHitToAnalytics(String currencyIds, String currencyNames) {
        Bundle payload = new Bundle();
        payload.putString(FirebaseAnalytics.Param.ITEM_ID, currencyIds);
        payload.putString(FirebaseAnalytics.Param.ITEM_NAME, currencyNames);
        payload.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "text/html");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);
    }

    public void openAlertDialog() {
        Alert alert = Utility.getAlertSettings(mContext);

        if (alert == null) {
            return;
        }

        if (alert.getCurrencyFrom() == null) {
            FirebaseCrash.report(new Exception("Alert Currency From is NULL"));
            return;
        }

        if (alert.getCurrencyTo() == null) {
            FirebaseCrash.report(new Exception("Alert Currency To is NULL"));
            return;
        }

        if (alert.isEnabled() && (!alert.getCurrencyFrom().getId().equals(mCurrencyFromId) || !alert.getCurrencyTo().getId().equals(mCurrencyToId))) {
            openExistingAlertDialog(alert.getCurrencyFrom().getId(), alert.getCurrencyTo().getId());
        } else {
            openCreateAlertDialog();
        }
    }

    private void openCreateAlertDialog() {
        Bundle arguments = new Bundle();
        arguments.putParcelable(DetailActivityFragment.CURRENCY_FROM, new Currency(mCurrencyFromId));
        arguments.putParcelable(DetailActivityFragment.CURRENCY_TO, new Currency(mCurrencyToId));

        AddAlertFragment fragment = new AddAlertFragment();
        fragment.setArguments(arguments);

        fragment.show(mContext.getSupportFragmentManager(), null);
    }

    private void openExistingAlertDialog(String currencyFrom, String currencyTo) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                mContext);

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.dialog_existing_alert, currencyFrom, currencyTo))
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        openCreateAlertDialog();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

}
