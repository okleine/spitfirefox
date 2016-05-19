package de.uzl.itm.ncoap.android.client;


import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


import java.net.URI;

import android.widget.Spinner;
import android.widget.Toast;
import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.options.UintOptionValue;


public class CoapClientActivity extends AppCompatActivity implements RequestFragment.SendRequestButtonClickedListener {

    private static long DISCOVER = 0;
    private static long PING = 5;

    private CoapClient clientApplication;
    private RequestFragment requestFragment;
    private ResponseFragment responseFragment;


    private ViewPager viewPager;
    private ScreenSlidePagerAdapter viewPagerAdapter;

    public CoapClient getClientApplication(){
        return this.clientApplication;
    }

    public ResponseFragment getResponseFragment(){
        return this.responseFragment;
    }

    public void setResponseFragment(ResponseFragment responseFragment){
        this.responseFragment = responseFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        //Initialize the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.spitfire_logo);
        toolbar.setSubtitle(R.string.app_subtitle);
        setSupportActionBar(toolbar);


        this.clientApplication = new CoapClient();
        this.requestFragment = new RequestFragment();
        this.responseFragment = new ResponseFragment();

        this.hideKeyboard();
    }

    @Override
    public void onBackPressed(){
        if(viewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSendButtonClicked() {
        this.hideKeyboard();

        long method = ((Spinner) findViewById(R.id.spn_methods)).getSelectedItemId();

        if(method == DISCOVER){
            new ServiceDiscoveryTask(this).execute();
        } else if(method == PING) {
            new SendPingTask(this).execute();
        } else {
            new SendRequestTask(this).execute(method);
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = this.getCurrentFocus();
        IBinder token;
        if(currentFocus == null) {
            token = null;
        } else {
            token = currentFocus.getWindowToken();
        }
        inputManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
    }


    public void processResponse(final CoapResponse coapResponse, final URI serviceURI, final long duration) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long block2Num = coapResponse.getBlock2Number();
                String text = "Response received";
                if (block2Num != UintOptionValue.UNDEFINED) {
                    text += " (" + block2Num + " blocks in " + duration + " ms)";
                } else {
                    text += " (after " + duration + " ms)";
                }

                Toast.makeText(CoapClientActivity.this, text, Toast.LENGTH_LONG).show();

                viewPager.setCurrentItem(1);
                CoapClientActivity.this.responseFragment.responseReceived(serviceURI, coapResponse);
            }
        });
    }

    /**
     * A simple pager adapter
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0){
                return requestFragment;
            }
            else{
                return new ResponseFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
