package de.uzl.itm.ncoap.android.client;


import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.URI;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.options.UintOptionValue;
import de.uzl.itm.client.R;


public class MainActivity extends FragmentActivity implements RequestFragment.SendRequestButtonClickedListener {

    private CoapClientApplication clientApplication;
    private RequestFragment requestFragment;
    private ResponseFragment responseFragment;


    private ViewPager viewPager;
    private ScreenSlidePagerAdapter viewPagerAdapter;

    public CoapClientApplication getClientApplication(){
        return this.clientApplication;
    }

    public ResponseFragment getResponseFragment(){
        return this.responseFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        this.clientApplication = new CoapClientApplication();
        this.requestFragment = new RequestFragment();
        this.responseFragment = new ResponseFragment();
    }

    @Override
    public void onBackPressed(){
        if(viewPager.getCurrentItem() == 0){
            super.onBackPressed();
        }
        else{
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
        //Discover
        if(method == 0){
            new ServiceDiscoveryTask(this).execute();
        }
        //Ping
        else if(method == 5){
            new SendPingTask(this).execute();
        }
        else{
            new SendRequestTask(this).execute();
        }
    }

    private void hideKeyboard(){
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = this.getCurrentFocus();
        IBinder token;
        if(currentFocus == null){
            token = null;
        }
        else{
            token = currentFocus.getWindowToken();
        }
        inputManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
    }


    public void processResponse(final CoapResponse coapResponse, final URI serviceURI, final long duration) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long blockNo = coapResponse.getBlock2Number();
                String text = "Response received";
                if (blockNo != UintOptionValue.UNDEFINED) {
                    text += " (" + blockNo + " blocks in " + duration + " ms)";
                } else {
                    text += " (after " + duration + " ms)";
                }

                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();

                viewPager.setCurrentItem(1);
                MainActivity.this.responseFragment.responseReceived(serviceURI, coapResponse);

            }
        });
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
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
                return responseFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
