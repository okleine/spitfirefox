package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uzl.itm.ncoap.message.*;

/**
 * Created by olli on 07.05.15.
 */
public class SendRequestTask extends AsyncTask<Long, Void, SendRequestTask.SpitfirefoxCallback>{

    private ProgressDialog progressDialog;
    private CoapClientActivity activity;
    private CoapClient coapClient;

    private String serverName, localUri, acceptedFormats, payloadFormat, payload;
    private int portNumber;
    private boolean confirmable, observe;


    public SendRequestTask(CoapClientActivity activity){
        this.activity = activity;
        this.coapClient = activity.getClientApplication();
        this.progressDialog = new ProgressDialog(this.activity);
    }


    @Override
    protected void onPreExecute(){

        this.serverName = ((EditText) activity.findViewById(R.id.txt_server)).getText().toString();

        try {
            this.portNumber = Integer.valueOf(((EditText) activity.findViewById(R.id.txt_port)).getText().toString());
        } catch(NumberFormatException ex) {
            this.portNumber = 5683;
        }

        this.localUri = ((AutoCompleteTextView) activity.findViewById(R.id.txt_service)).getText().toString();
        this.confirmable = ((RadioButton) activity.findViewById(R.id.rad_con)).isChecked();
        this.observe = ((CheckBox) activity.findViewById(R.id.chk_observation)).isChecked();
        this.acceptedFormats = ((EditText) activity.findViewById(R.id.txt_accept)).getText().toString();
        this.payloadFormat = ((EditText) activity.findViewById(R.id.txt_contentformat)).getText().toString();
        this.payload = ((EditText) activity.findViewById(R.id.txt_payload)).getText().toString();

        progressDialog.setMessage(this.activity.getResources().getString(R.string.waiting));
        progressDialog.show();
    }


    @Override
    protected void onPostExecute(SpitfirefoxCallback clientCallback){
        activity.getResponseFragment().setClientCallback(activity, clientCallback);
    }


    @Override
    protected SpitfirefoxCallback doInBackground(Long... method){
        try{
            //Read server name from UI
            //String serverName = ((EditText) activity.findViewById(R.id.txt_server)).getText().toString();
            if("".equals(serverName)){
                showToast("Enter Server (Host or IP)");
                return null;
            }

            //Create socket address from server name and port
            InetSocketAddress remoteEndpoint = new InetSocketAddress(InetAddress.getByName(serverName), portNumber);

            //Read CON/NON from UI
            MessageType.Name messageType;
            //if(((RadioButton) activity.findViewById(R.id.rad_con)).isChecked()){
            if(confirmable){
                messageType = MessageType.Name.CON;
            } else {
                messageType = MessageType.Name.NON;
            }

            //Create URI from server name, port and service path (and query)
            URI serviceURI = new URI("coap", null, serverName, remoteEndpoint.getPort(), localUri, null, null);

            //Define method to be set in request
            MessageCode.Name messageCode = MessageCode.Name.UNKNOWN;
            if(method[0] == 1) {
                messageCode = MessageCode.Name.GET;
            } else if(method[0] == 2) {
                messageCode = MessageCode.Name.POST;
            } else if(method[0] == 3) {
                messageCode = MessageCode.Name.PUT;
            } else if(method[0] == 4) {
                messageCode = MessageCode.Name.DELETE;
            }

            //Create initial CoAP request
            CoapRequest coapRequest = new CoapRequest(messageType, messageCode, serviceURI);

            //Set observe option (for GET only)
            if(observe && method[0] != 1) {
                this.progressDialog.dismiss();
                showToast("Use GET for observation!");
                return null;
            } else if (observe) {
                coapRequest.setObserve(0);
            }

            //Set accept option values in request (if any)
            if(!("".equals(this.acceptedFormats))){
                String[] array = this.acceptedFormats.split(";");
                long[] acceptOptionValues = new long[array.length];
                for(int i = 0; i < acceptOptionValues.length; i++) {
                    if(!("".equals(array[i]))) {
                        acceptOptionValues[i] = Long.valueOf(array[i]);
                    }
                }
                coapRequest.setAccept(acceptOptionValues);
            }

            //Set payload and payload related options in request (if any)
            if(!("".equals(this.payload)) && "".equals(this.payloadFormat)){
                this.progressDialog.dismiss();
                showToast("No Content Type for payload defined!");
                return null;
            } else if (!("".equals(payloadFormat))){
                coapRequest.setContent(payload.getBytes(CoapMessage.CHARSET), Long.valueOf(payloadFormat));
            }

            //Create callback and send request
            SpitfirefoxCallback clientCallback = new SpitfirefoxCallback(
                    serviceURI, coapRequest.isObservationRequest()
            );
            this.coapClient.sendCoapRequest(coapRequest, clientCallback, remoteEndpoint);

            return clientCallback;
        } catch (final Exception e) {
            this.progressDialog.dismiss();
            showToast(e.getMessage());
            return null;
        }

    }


    private void showToast(final String text){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public class SpitfirefoxCallback extends ClientCallback {

        private URI serviceURI;
        private int retransmissionCounter;
        private long startTime;
        private boolean observationCancelled;

        private SpitfirefoxCallback(URI serviceURI, boolean observation) {
            this.serviceURI = serviceURI;
            this.retransmissionCounter = 0;
            this.startTime = System.currentTimeMillis();
            this.observationCancelled = !observation;
        }


        @Override
        public void processCoapResponse(CoapResponse coapResponse) {
            long duration = System.currentTimeMillis() - startTime;
            progressDialog.dismiss();
            activity.processResponse(coapResponse, this.serviceURI, duration);
        }

        @Override
        public void processEmptyAcknowledgement(){
            showToast("Empty ACK received!");
        }

        @Override
        public void processReset(){
            progressDialog.dismiss();
            showToast("RST received from Server!");
        }

        @Override
        public void processTransmissionTimeout(){
            progressDialog.dismiss();
            showToast("Transmission timed out!");
        }

        @Override
        public void processRetransmission(){
            showToast("Retransmission No. " + (retransmissionCounter++));
        }

        @Override
        public void processMiscellaneousError(final String description) {
            progressDialog.dismiss();
            showToast("ERROR: " + description + "!");
        }

        public void cancelObservation(){
            this.observationCancelled = true;
        }

        @Override
        public boolean continueObservation(){
            return !this.observationCancelled;
        }
    }
}

