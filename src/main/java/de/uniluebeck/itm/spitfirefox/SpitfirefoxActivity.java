package de.uniluebeck.itm.spitfirefox;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;


public class SpitfirefoxActivity extends Activity implements CoapResponseProcessor, RetransmissionProcessor,
        RetransmissionTimeoutProcessor {

    private static String TAG = "nCoap.application.android.spitfirefox";

    private Handler handler = new Handler();

    private Button sendButton;
    private LinearLayout waitForResponse;
    private Button cancelButton;
    private EditText uriTextBox;
    private EditText payloadTextBox;
    private RadioGroup methodGroup;
    private RadioGroup msgTypeGroup;

    private CoapClientApplication clientApplication;
    private int retransmissionCounter;

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

        clientApplication = new CoapClientApplication();

        //Log.i(TAG, "onCreate");
        setContentView(R.layout.main);

        log.info("CREATED!");
        log.info("Available processors: {}", Runtime.getRuntime().availableProcessors());
         //initialize view objects
        sendButton = (Button) findViewById(R.id.btn_send);
        cancelButton = (Button) findViewById(R.id.btn_cancel);
        waitForResponse = (LinearLayout) findViewById(R.id.progress);
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

        uriTextBox.setText("141.83.68.39:5683/.well-known/core");

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
                retransmissionCounter = 0;
                new CommunicationTask().execute();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        retransmissionCounter = 0;
                        waitForResponse.setVisibility(View.GONE);
                        sendButton.setVisibility(View.VISIBLE);
                        payloadTextBox.setText("Request canceled.");
                    }
                });
            }
        });

    }

    @Override
    public void processCoapResponse(final CoapResponse coapResponse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                retransmissionCounter = 0;
                waitForResponse.setVisibility(View.GONE);
                sendButton.setVisibility(View.VISIBLE);
                payloadTextBox.setText(new String(coapResponse.getPayload().array(),
                        Charset.forName("UTF-8")));
            }
        });
    }

    @Override
    public void requestSent() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                retransmissionCounter++;
                payloadTextBox.setText("Request sent #" + retransmissionCounter);
            }
        });
    }

    @Override
    public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage) {
        handler.post(new Runnable(){
            @Override
            public void run() {
                waitForResponse.setVisibility(View.GONE);
                sendButton.setVisibility(View.VISIBLE);
                retransmissionCounter = 0;
                payloadTextBox.setText("Retransmission Timeout...");
            }
        });
    }



    private class CommunicationTask extends AsyncTask<Object, Integer, Object>{

        @Override
        protected void onPreExecute(){
            sendButton.setVisibility(View.GONE);
            waitForResponse.setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Object... params) {
            try {
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

                CoapRequest coapRequest = new CoapRequest(msgType, code, new URI(targetURI));

                log.info("Write Request: " + coapRequest);

                clientApplication.writeCoapRequest(coapRequest, SpitfirefoxActivity.this);


            } catch (final Exception e){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        payloadTextBox.setText("Exception in communication:\n" + e.getMessage());
                        log.error("Exception at some point!", e);
                    }
                });
            }
            return "Fertig";
        }
    }
}

