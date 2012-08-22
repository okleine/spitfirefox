package de.uniluebeck.itm.spitfirefox;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.CoapResponse;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;

public class SpitfirefoxActivity extends Activity{

    private static String TAG = "nCoap.application.android.spitfirefox";

    Handler handler = new Handler();
    private Button sendButton;
    private ProgressBar progressBar;
    private EditText uriTextBox;
    private EditText payloadTextBox;
    private RadioGroup methodGroup;
    private RadioGroup msgTypeGroup;

    private Logger log = LoggerFactory.getLogger(SpitfirefoxActivity.class.getName());
    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG, "onCreate");
        setContentView(R.layout.main);

        log.debug("CREATED!");

         //initialize view objects
        sendButton = (Button) findViewById(R.id.btn_send);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        uriTextBox = (EditText) findViewById(R.id.txt_uri);
        payloadTextBox = (EditText) findViewById(R.id.payload);
        methodGroup = (RadioGroup) findViewById(R.id.rdbg_method);
        msgTypeGroup = (RadioGroup) findViewById(R.id.rdbg_reliability);


        //Set GET as default method
        RadioButton getButton = (RadioButton) findViewById(R.id.rdb_get);
        getButton.setChecked(true);

        //Set CON as default reliability level
        RadioButton conButton = (RadioButton) findViewById(R.id.rdb_con);
        conButton.setChecked(true);

        //Change available options when method changes

        methodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                String text;
                switch (checkedId){
                    case R.id.rdb_get:
                        text = "GET";
                        break;
                    case R.id.rdb_put:
                        text = "PUT";
                        break;
                    case R.id.rdb_post:
                        text = "POST";
                        break;
                    case R.id.rdb_delete:
                        text = "DELETE";
                        break;
                    default:
                        text = "No method selected!";
                }

                payloadTextBox.setText(text);
            }
        });



        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CommunicationTask().execute();
            }
        });

    }

    private class CommunicationTask extends AsyncTask<Object, Integer, Object>{

        @Override
        protected void onPreExecute(){
            sendButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Object... params) {
            try {
                CoapClientApplication clientApplication = new CoapClientApplication(){

                    @Override
                    public void receiveCoapResponse(final CoapResponse coapResponse) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                sendButton.setVisibility(View.VISIBLE);
                                payloadTextBox.setText(coapResponse.getPayload().toString(Charset.forName("UTF-8")));
                            }
                        });
                    }
                };

                //Create request
                MsgType msgType = msgTypeGroup.getCheckedRadioButtonId() == R.id.rdb_con ? MsgType.CON : MsgType.NON;

                Code code = null;
                switch (methodGroup.getCheckedRadioButtonId()){
                    case R.id.rdb_get:
                        code = Code.GET;
                        break;
                    case R.id.rdb_put:
                        code = Code.PUT;
                        break;
                    case R.id.rdb_post:
                        code = Code.POST;
                        break;
                    case R.id.rdb_delete:
                        code = Code.DELETE;
                        break;
                }

                String targetURI = uriTextBox.getText().toString();
                if(!targetURI.startsWith("coap://")){
                    targetURI = "coap://" + targetURI;
                }

                final String finalTargetURI = targetURI;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        uriTextBox.setText(finalTargetURI);                                            }
                });

                CoapRequest coapRequest = new CoapRequest(msgType, code, new URI(targetURI), clientApplication);


                //Send request
                clientApplication.writeCoapRequest(coapRequest);

            } catch (final Exception e){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        payloadTextBox.setText("Exception in communication:\n" + e.getMessage());
                    }
                });
            }
            return "Fertig";
        }
    }
}

