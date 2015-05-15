package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.android.client.MainActivity;

/**
 * Created by olli on 07.05.15.
 */
public class SendRequestTask extends AsyncTask<Void, Void, SendRequestTask.AndroidClientCallback>{

    private ProgressDialog progressDialog;
    private MainActivity mainActivity;
    private CoapClientApplication clientApplication;


    public SendRequestTask(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        this.clientApplication = mainActivity.getClientApplication();
        this.progressDialog = new ProgressDialog(this.mainActivity);
    }


    @Override
    protected void onPreExecute(){
        progressDialog.setMessage(this.mainActivity.getResources().getString(R.string.waiting));
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(AndroidClientCallback clientCallback){
        mainActivity.getResponseFragment().setClientCallback(clientCallback);
    }

    @Override
    protected AndroidClientCallback doInBackground(Void... nothing){
        try{
            //Read server name from UI
            String serverName = ((EditText) mainActivity.findViewById(R.id.txt_server)).getText().toString();
            if("".equals(serverName)){
                showToast("Enter Server (Host or IP)");
                return null;
            }

            //Read port from UI
            EditText txtPort = ((EditText) mainActivity.findViewById(R.id.txt_port));
            int port;
            if("".equals(txtPort.getText().toString())){
                port = 5683;
            }
            else {
                port = Integer.valueOf(txtPort.getText().toString());
            }

            //Create socket address from server name and port
            InetSocketAddress remoteEndpoint = new InetSocketAddress(InetAddress.getByName(serverName), port);

            //Read CON/NON from UI
            MessageType.Name messageType;
            if(((RadioButton) mainActivity.findViewById(R.id.rad_con)).isChecked()){
                messageType = MessageType.Name.CON;
            }
            else{
                messageType = MessageType.Name.NON;
            }

            //Read service from UI
            AutoCompleteTextView txtService = (AutoCompleteTextView) mainActivity.findViewById(R.id.txt_service);
            String localURI = txtService.getText().toString();

            //Create URI from server name, port and service path (and query)
            URI serviceURI = new URI("coap", null, serverName, remoteEndpoint.getPort(), localURI, null, null);

            //Read method from UI
            MessageCode.Name messageCode = MessageCode.Name.UNKNOWN;

            long method = ((Spinner) mainActivity.findViewById(R.id.spn_methods)).getSelectedItemId();
            if(method == 1){
                messageCode = MessageCode.Name.GET;
            }
            else if(method == 2){
                messageCode = MessageCode.Name.POST;
            }
            else if(method == 3){
                messageCode = MessageCode.Name.PUT;
            }
            else if(method == 4){
                messageCode = MessageCode.Name.DELETE;
            }

            //Create initial CoAP request
            CoapRequest coapRequest = new CoapRequest(messageType, messageCode, serviceURI);


            //Read OBS from UI
            CheckBox chkObserve = ((CheckBox) mainActivity.findViewById(R.id.chk_observation));
            if(chkObserve.isChecked()){
                if(method != 1){
                    showToast("Use GET for observation!");
                    return null;
                }
                coapRequest.setObserve(0);
            }

            //Create accept options
            String strAcceptValues = ((EditText) mainActivity.findViewById(R.id.txt_accept)).getText().toString();
            if(!("".equals(strAcceptValues))){
                String[] array = strAcceptValues.split(";");
                long[] acceptValues = new long[array.length];
                for(int i = 0; i < acceptValues.length; i++){
                    if(!("".equals(array[i]))){
                        acceptValues[i] = Long.valueOf(array[i]);
                    }
                }
                coapRequest.setAccept(acceptValues);
            }

            //Read payload and content type from UI
            String payload = ((EditText) mainActivity.findViewById(R.id.txt_payload)).getText().toString();
            String contentFormat = ((EditText) mainActivity.findViewById(R.id.txt_contenttype)).getText().toString();

            if(!("".equals(payload)) && "".equals(contentFormat)){
                this.progressDialog.dismiss();
                showToast("No Content Type for payload defined!");
                return null;
            }
            else if (!("".equals(contentFormat))){
                coapRequest.setContent(payload.getBytes(CoapMessage.CHARSET), Long.valueOf(contentFormat));
            }

            AndroidClientCallback clientCallback = new AndroidClientCallback(serviceURI, coapRequest.isObservationRequest());

            //set new callback for response fragment

            this.clientApplication.sendCoapRequest(coapRequest, clientCallback, remoteEndpoint);

            return clientCallback;
        }
        catch (final Exception e) {
            this.progressDialog.dismiss();
            showToast(e.getMessage());
            return null;
        }

    }


    private void showToast(final String text){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public class AndroidClientCallback extends ClientCallback{

        private URI serviceURI;
        private int retransmissionCounter;
        private long startTime;
        private boolean observationCancelled;

        private AndroidClientCallback(URI serviceURI, boolean observation) {
            this.serviceURI = serviceURI;
            this.retransmissionCounter = 0;
            this.startTime = System.currentTimeMillis();
            this.observationCancelled = !observation;
        }


        @Override
        public void processCoapResponse(CoapResponse coapResponse) {
            long duration = System.currentTimeMillis() - startTime;
            progressDialog.dismiss();
            mainActivity.processResponse(coapResponse, this.serviceURI, duration);
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

