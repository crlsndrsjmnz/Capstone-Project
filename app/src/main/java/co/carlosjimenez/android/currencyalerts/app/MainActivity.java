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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

/**
 * Activity for the main view UI
 */
public class MainActivity extends AppCompatActivity implements MainActivityFragment.Callback {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT";

    private MainActivityFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {

            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            if (fragment == null) {
                fragment = new MainActivityFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.flMain, fragment, MAIN_FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void onItemSelected(Uri rateUri, ImageView imageFrom, ForexAdapter.ForexAdapterViewHolder vh) {
        Intent intent = new Intent(this, DetailActivity.class)
                .setData(rateUri);

        Pair<View, String> taFlagFrom = Pair.create((View)imageFrom, getString(R.string.detail_flag_from_transition_name));
        Pair<View, String> taFlagTo = Pair.create((View)vh.mIconView, getString(R.string.detail_flag_to_transition_name));

        ActivityOptionsCompat activityOptions =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                        taFlagFrom, taFlagTo);

        ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
    }

    @Override
    public void onSettingsSelected() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
