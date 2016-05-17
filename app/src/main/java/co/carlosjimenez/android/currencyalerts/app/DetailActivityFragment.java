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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.carlosjimenez.android.currencyalerts.app.data.ForexContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    static final String DETAIL_URI = "URI";
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
    private static final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
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
    private Uri mUri;
    private AppCompatActivity mContext;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, rootView);

        if (mToolbar != null) {
            mContext.setSupportActionBar(mToolbar);
            mContext.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mContext.getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

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

            MenuTint.colorMenuItem(menu.getItem(0), getResources().getColor(R.color.detail_toolbar_icon_color), null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_alert) {
            Toast.makeText(mContext, "Alert", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_share) {
            Toast.makeText(mContext, "Share", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            Log.d(LOG_TAG, "Forex Loader Finished: No data returned");

            return;
        }
        if (!data.moveToFirst()) {
            Log.d(LOG_TAG, "Forex Loader Finished: No data returned");

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

        Glide.with(getActivity())
                .load(data.getString(COL_COUNTRY_FROM_FLAG))
                .error(R.drawable.generic)
                .crossFade()
                .into(mIvFlagFrom);

        Glide.with(getActivity())
                .load(data.getString(COL_COUNTRY_TO_FLAG))
                .error(R.drawable.generic)
                .crossFade()
                .into(mIvFlagTo);

        mTvCurrencyFromDesc.setText(data.getString(COL_CURRENCY_FROM_NAME));
        mTvCurrencyFromRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_FROM_SYMBOL), 1));
        mTvCurrencyToDesc.setText(data.getString(COL_CURRENCY_TO_NAME));
        mTvCurrencyToRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), data.getDouble(COL_RATE_VAL)));

        Time dayTime = new Time();
        dayTime.setToNow();

        int julianDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        dayTime = new Time();
        long lMinDate = dayTime.setJulianDay(julianDate - DEFAULT_DAYS_FOREX_AVERAGE);

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

        if (DEFAULT_DAYS_FOREX_AVERAGE > 1)
            mTvPeriod.setText(DEFAULT_DAYS_FOREX_AVERAGE + " days");
        else
            mTvPeriod.setText(DEFAULT_DAYS_FOREX_AVERAGE + " day");

        mTvMaxRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), dMaxVal));
        mTvMinRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), dMinVal));
        mTvAverageRate.setText(Utility.formatCurrencyRate(getActivity(), data.getString(COL_CURRENCY_TO_SYMBOL), dRateAverage));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
