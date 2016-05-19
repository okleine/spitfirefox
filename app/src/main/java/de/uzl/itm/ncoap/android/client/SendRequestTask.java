package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.*;
import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Created by olli on 07.05.15.
 */
public class SendRequestTask extends AsyncTask<Long, Void, SendRequestTask.SpitfirefoxCallback>{

    private ProgressDialog progressDialog;
    private CoapClientActivity activity;
    private CoapClient coapClient;

    private String serverName, localUri, acceptedFormats, payloadFormat, payload, ifMatch, etags;
    private int portNumber;
    private boolean confirmable, observe;
    private BlockSize block1Size, block2Size;

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
        this.ifMatch = ((EditText)activity.findViewById(R.id.txt_ifmatch)).getText().toString();
        this.etags = ((EditText) activity.findViewById(R.id.txt_etag_request)).getText().toString();

        this.block2Size = this.getBlock2Size();
        this.block1Size = this.getBlock1Size();

        progressDialog.setMessage(this.activity.getResources().getString(R.string.waiting));
        progressDialog.show();
    }

    private BlockSize getBlock2Size() {
        long block2Szx= ((Spinner) activity.findViewById(R.id.spn_block2)).getSelectedItemId() - 1;
        return BlockSize.getBlockSize(block2Szx);
    }

    private BlockSize getBlock1Size() {
        long block1Szx= ((Spinner) activity.findViewById(R.id.spn_block1)).getSelectedItemId() - 1;
        return BlockSize.getBlockSize(block1Szx);
    }

    private byte[][] getOpaqueOptionValues(String hex) throws IllegalArgumentException{
        if(!"".equals(hex)) {
            String[] tmp = hex.split(";");
            byte[][] result = new byte[tmp.length][];
            for (int i = 0; i < tmp.length; i++) {
                result[i] = hexStringToByteArray(tmp[i]);
            }
            return result;
        } else {
            return new byte[0][];
        }
    }

    private static byte[] hexStringToByteArray(String hex) throws IllegalArgumentException {
        // add leading zero if necessary
        hex = hex.length() % 2 == 0 ? hex : "0" + hex;

        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int leftBits = Character.digit(hex.charAt(i), 16);
            int rightBits = Character.digit(hex.charAt(i+1), 16);
            if(leftBits == -1 || rightBits == -1) {
                throw new IllegalArgumentException("No HEX value: '" + hex.charAt(i) + hex.charAt(i+1) + "'");
            } else {
                data[i / 2] = (byte) ((leftBits << 4) + rightBits);
            }
        }
        return data;
    }

    @Override
    protected void onPostExecute(SpitfirefoxCallback clientCallback){
        activity.getResponseFragment().setClientCallback(activity, clientCallback);
    }


    @Override
    protected SpitfirefoxCallback doInBackground(Long... method){
        try{
            if("".equals(serverName)) {
                showLongToast("Enter Server (Host or IP)");
                return null;
            }

            //Create socket address from server name and port
            InetSocketAddress remoteEndpoint = new InetSocketAddress(InetAddress.getByName(serverName), portNumber);

            //Read CON/NON from UI
            int messageType;
            //if(((RadioButton) activity.findViewById(R.id.rad_con)).isChecked()){
            if(confirmable) {
                messageType = MessageType.CON;
            } else {
                messageType = MessageType.NON;
            }

            //Create URI from server name, port and service path (and query)
            URI serviceURI = new URI("coap", null, serverName, remoteEndpoint.getPort(), localUri, null, null);


            //Create initial CoAP request
            CoapRequest coapRequest = new CoapRequest(messageType, method[0].intValue(), serviceURI);

            //Set if-match option values (if any)
            try {
                coapRequest.setIfMatch(getOpaqueOptionValues(this.ifMatch));
            } catch (IllegalArgumentException ex) {
                this.progressDialog.dismiss();
                showLongToast("Malformed IF-MATCH: " + ex.getMessage());
                return null;
            }


            //Set etag option values (if any)
            try {
                coapRequest.setEtags(getOpaqueOptionValues(this.etags));
            } catch (IllegalArgumentException ex) {
                this.progressDialog.dismiss();
                showLongToast("Malformed ETAG: " + ex.getMessage());
                return null;
            }

            //Set observe option (for GET only)
            if(observe && method[0] != 1) {
                this.progressDialog.dismiss();
                showLongToast("Use GET for observation!");
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

            //Set block options (if any)
            if(BlockSize.UNBOUND != this.block1Size) {
                coapRequest.setPreferredBlock1Size(this.block1Size);
            }

            if(BlockSize.UNBOUND != this.block2Size) {
                coapRequest.setPreferredBlock2Size(this.block2Size);
            }

            //Set payload and payload related options in request (if any)
            if(!("".equals(this.payload)) && "".equals(this.payloadFormat)){
                this.progressDialog.dismiss();
                showLongToast("No Content Type for payload defined!");
                return null;
            } else if (!("".equals(payloadFormat))){
                coapRequest.setContent(payload.getBytes(CoapMessage.CHARSET), Long.valueOf(payloadFormat));
            }


            //Create callback and send request
            SpitfirefoxCallback clientCallback = new SpitfirefoxCallback(
                    serviceURI, coapRequest.isObservationRequest()
            );
            this.coapClient.sendCoapRequest(coapRequest, remoteEndpoint, clientCallback);

            return clientCallback;
        } catch (final Exception e) {
            this.progressDialog.dismiss();
            showLongToast(e.getMessage());
            return null;
        }
    }


    private void showLongToast(final String text){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
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
            showLongToast("Empty ACK received!");
        }

        @Override
        public void processReset(){
            progressDialog.dismiss();
            showLongToast("RST received from Server!");
        }

        @Override
        public void processTransmissionTimeout(){
            progressDialog.dismiss();
            showLongToast("Transmission timed out!");
        }

        @Override
        public void processResponseBlockReceived(final long receivedLength, final long expectedLength) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String expected = expectedLength == -1 ? "UNKNOWN" : ("" + expectedLength);
                    progressDialog.setMessage(receivedLength + " / " + expected);
                }
            });
        }

        @Override
        public void processRetransmission(){
            showLongToast("Retransmission No. " + (retransmissionCounter++));
        }

        @Override
        public void processMiscellaneousError(final String description) {
            progressDialog.dismiss();
            showLongToast("ERROR: " + description + "!");
        }

        public void cancelObservation(){
            this.observationCancelled = true;
        }

        @Override
        public boolean continueObservation() {
            return !this.observationCancelled;
        }
    }
}

