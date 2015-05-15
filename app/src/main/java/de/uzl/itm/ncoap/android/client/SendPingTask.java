package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.android.client.MainActivity;

/**
 * Created by olli on 07.05.15.
 */
public class SendPingTask extends AsyncTask<Void, Void, Void>{

    private MainActivity mainActivity;
    private CoapClientApplication clientApplication;
    private ProgressDialog progressDialog;

    public SendPingTask(MainActivity MainActivity){
        this.mainActivity = MainActivity;
        this.clientApplication = this.mainActivity.getClientApplication();
        this.progressDialog = new ProgressDialog(this.mainActivity);
    }

    @Override
    protected void onPreExecute(){
        progressDialog.setMessage(
                this.mainActivity.getResources().getString(R.string.waiting)
        );
        progressDialog.show();
    }


    @Override
    protected Void doInBackground(Void... nothing) {
        String serverName = ((EditText) mainActivity.findViewById(R.id.txt_server)).getText().toString();

        if("".equals(serverName)){
            showToast("Enter Server (Host or IP)");
            return null;
        }

        final InetSocketAddress remoteEndpoint;
        try{
            EditText txtPort = ((EditText) mainActivity.findViewById(R.id.txt_port));
            int port;
            if("".equals(txtPort.getText().toString())){
                port = 5683;
            }
            else {
                port = Integer.valueOf(txtPort.getText().toString());
            }
            remoteEndpoint = new InetSocketAddress(InetAddress.getByName(serverName), port);
        }
        catch (final Exception e) {
            showToast(e.getMessage());
            return null;
        }

        clientApplication.sendCoapPing(new PingCallback(remoteEndpoint), remoteEndpoint);

        return null;
    }

    private void showToast(final String text){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class PingCallback extends ClientCallback{

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

