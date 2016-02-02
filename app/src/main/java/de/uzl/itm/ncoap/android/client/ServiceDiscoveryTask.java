/**
 * Created by olli on 13.05.15.
 */
package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;


import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.client.linkformat.LinkFormatDecoder;
import de.uzl.itm.ncoap.application.server.webresource.linkformat.LinkAttribute;
import de.uzl.itm.ncoap.communication.dispatching.client.ClientCallback;
import de.uzl.itm.ncoap.message.*;


/**
 * Created by olli on 07.05.15.
 */
public class ServiceDiscoveryTask extends AsyncTask<Void, Void, Void> {

    private CoapClientActivity activity;
    private CoapClient coapClient;
    private ProgressDialog progressDialog;

    private String serverName;
    private int portNumber;
    private boolean confirmable;

    public ServiceDiscoveryTask(CoapClientActivity activity){
        this.activity = activity;
        this.coapClient = this.activity.getClientApplication();
        this.progressDialog = new ProgressDialog(this.activity);
    }


    @Override
    protected void onPreExecute(){
        //Read server name from UI
        this.serverName = ((EditText) activity.findViewById(R.id.txt_server)).getText().toString();

        try {
            this.portNumber = Integer.valueOf(((EditText) activity.findViewById(R.id.txt_port)).getText().toString());
        } catch(NumberFormatException ex) {
            this.portNumber = 5683;
        }

        this.confirmable = ((RadioButton) activity.findViewById(R.id.rad_con)).isChecked();

        progressDialog.setMessage(
                this.activity.getResources().getString(R.string.waiting)
        );
        progressDialog.show();
    }


    @Override
    protected Void doInBackground(final Void... nothing) {
        try {

            if("".equals(serverName)) {
                showToast("Enter Server (Host or IP)");
                return null;
            }

            //Create socket address from server name and port
            InetSocketAddress remoteEndpoint = new InetSocketAddress(InetAddress.getByName(serverName), portNumber);

            //Read CON/NON from UI
            MessageType.Name messageType;
            if(this.confirmable){
                messageType = MessageType.Name.CON;
            }
            else{
                messageType = MessageType.Name.NON;
            }

            //Create CoAP request to discover resources
            URI targetURI = new URI("coap", null, serverName, portNumber, "/.well-known/core", null, null);
            CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.Name.GET, targetURI);

            this.coapClient.sendCoapRequest(coapRequest, new ServiceDiscoveryCallback(), remoteEndpoint);

            return null;

        }
        catch (final Exception e) {
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


    private class ServiceDiscoveryCallback extends ClientCallback {

        @Override
        public void processCoapResponse(final CoapResponse coapResponse) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();

                    String payload = coapResponse.getContent().toString(CoapMessage.CHARSET);
                    Map<String, Set<LinkAttribute>> services = LinkFormatDecoder.decode(payload);

                    String[] serviceNames = new String[services.keySet().size()];
                    serviceNames = services.keySet().toArray(serviceNames);
                    Arrays.sort(serviceNames);

                    showToast("Found " + serviceNames.length + " Services!");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                            android.R.layout.simple_spinner_dropdown_item, serviceNames);

                    AutoCompleteTextView txtService = ((AutoCompleteTextView) activity.findViewById(R.id.txt_service));
                    txtService.setAdapter(adapter);
                    txtService.setHint(R.string.service_hint2);


                    //Set Method Spinner to GET
                    ((Spinner) activity.findViewById(R.id.spn_methods)).setSelection(1);


                }
            });
        }


        @Override
        public void processMiscellaneousError(final String description) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("ERROR: " + description + "!");
                }
            });
        }

    }
}

