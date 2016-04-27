package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;
import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.message.CoapResponse;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by olli on 07.05.15.
 */
public class SendPingTask extends AsyncTask<Void, Void, Void>{

    private CoapClientActivity activity;
    private CoapClient clientApplication;
    private ProgressDialog progressDialog;

    private String serverName;
    private int portNumber;

    public SendPingTask(CoapClientActivity activity){
        this.activity = activity;
        this.clientApplication = activity.getClientApplication();
        this.progressDialog = new ProgressDialog(activity);
    }

    @Override
    protected void onPreExecute(){
        this.serverName = ((EditText) activity.findViewById(R.id.txt_server) ).getText().toString();

        try {
            this.portNumber = Integer.valueOf(((EditText) activity.findViewById(R.id.txt_port)).getText().toString());
        } catch(NumberFormatException ex) {
            this.portNumber = 5683;
        }

        progressDialog.setMessage(
                this.activity.getResources().getString(R.string.waiting)
        );
        progressDialog.show();
    }


    @Override
    protected Void doInBackground(Void... params) {

        //String serverName = ((EditText) activity.findViewById(R.id.txt_server)).getText().toString();

        if("".equals(serverName)){
            showToast("Enter Server (Host or IP)");
            return null;
        }

        final InetSocketAddress remoteEndpoint;
        try {
            remoteEndpoint = new InetSocketAddress(InetAddress.getByName(serverName), portNumber);
        } catch (final Exception e) {
            showToast(e.getMessage());
            return null;
        }

        clientApplication.sendCoapPing(remoteEndpoint, new PingCallback(remoteEndpoint));

        return null;
    }

    private void showToast(final String text){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class PingCallback extends ClientCallback {

        private InetSocketAddress remoteEndpoint;
        private long timeSent;
        private int counter;

        public PingCallback(InetSocketAddress remoteEndpoint){
            this.remoteEndpoint = remoteEndpoint;
            this.timeSent = System.currentTimeMillis();
            this.counter = 1;
        }

        @Override
        public void processCoapResponse(CoapResponse coapResponse) {
            //Nothing to do...
        }

        @Override
        public void processReset() {
            long timeReceived = System.currentTimeMillis();
            progressDialog.dismiss();
            showToast("PONG " + " received after " + (timeReceived - timeSent) + "ms\n" +
                    "(from " + remoteEndpoint.getHostName() + ")");
        }

        @Override
        public void processTransmissionTimeout(){
            progressDialog.dismiss();
            showToast("Server did not respond \n" + remoteEndpoint.getHostName());
        }

        @Override
        public void processRetransmission(){
            showToast("PING No. " + (++counter) + " sent to " + remoteEndpoint.getHostName() + "!");
        }

        @Override
        public void processEmptyAcknowledgement(){
            long timeReceived = System.currentTimeMillis();
            progressDialog.dismiss();
            showToast("Empty ACK (should have been RST) from " + remoteEndpoint.getHostName() +
                    " received after " + (timeReceived - timeSent) + "ms!");
        }

        @Override
        public void processMiscellaneousError(final String description) {
            progressDialog.dismiss();
            showToast("ERROR: " + description + "!");
        }
    }
}

