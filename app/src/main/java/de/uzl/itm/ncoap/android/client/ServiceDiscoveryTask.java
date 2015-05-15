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

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.linkformat.LinkFormatDecoder;
import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LinkAttribute;
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
public class ServiceDiscoveryTask extends AsyncTask<Void, Void, Void> {

    private MainActivity mainActivity;
    private CoapClientApplication clientApplication;
    private ProgressDialog progressDialog;

    public ServiceDiscoveryTask(MainActivity MainActivity){
        this.mainActivity = MainActivity;
        this.clientApplication = this.mainActivity.getClientApplication();
        this.progressDialog = new ProgressDialog(this.mainActivity);
    }

    @Override
    protected Void doInBackground(final Void... nothing) {

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

            //Create CoAP request to discover resources
            URI targetURI = new URI("coap", null, serverName, port, "/.well-known/core", null, null);
            CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.Name.GET, targetURI);

            this.clientApplication.sendCoapRequest(coapRequest, new ServiceDiscoveryCallback(), remoteEndpoint);

            return null;

        }
        catch (final Exception e) {
            this.progressDialog.dismiss();
            showToast(e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPreExecute(){
        progressDialog.setMessage(
                this.mainActivity.getResources().getString(R.string.waiting)
        );
        progressDialog.show();
    }


    private void showToast(final String text){
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private class ServiceDiscoveryCallback extends ClientCallback{

        @Override
        public void processCoapResponse(final CoapResponse coapResponse) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();

                    String payload = coapResponse.getContent().toString(CoapMessage.CHARSET);
                    Map<String, Set<LinkAttribute>> services = LinkFormatDecoder.decode(payload);

                    String[] serviceNames = new String[services.keySet().size()];
                    serviceNames = services.keySet().toArray(serviceNames);
                    Arrays.sort(serviceNames);

                    showToast("Found " + serviceNames.length + " Services!");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(mainActivity,
                            android.R.layout.simple_spinner_dropdown_item, serviceNames);

                    AutoCompleteTextView txtService = ((AutoCompleteTextView) mainActivity.findViewById(R.id.txt_service));
                    txtService.setAdapter(adapter);
                    txtService.setHint(R.string.service_hint2);


                    //Set Method Spinner to GET
                    ((Spinner) mainActivity.findViewById(R.id.spn_methods)).setSelection(1);


                }
            });
        }


        @Override
        public void processMiscellaneousError(final String description) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("ERROR: " + description + "!");
                }
            });
        }

    }
}

