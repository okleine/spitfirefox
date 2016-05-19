/**
 * Created by olli on 13.05.15.
 */
package de.uzl.itm.ncoap.android.client;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.*;
import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.application.client.CoapClient;
import de.uzl.itm.ncoap.application.linkformat.LinkValueList;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.message.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;


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
    private BlockSize block2Size;

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

        this.block2Size = this.getBlock2Size();

        progressDialog.setMessage(
                this.activity.getResources().getString(R.string.waiting)
        );
        progressDialog.show();
    }

    private BlockSize getBlock2Size() {
        long block2Szx= ((Spinner) activity.findViewById(R.id.spn_block2)).getSelectedItemId() - 1;
        return BlockSize.getBlockSize(block2Szx);
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
            int messageType;
            if(this.confirmable){
                messageType = MessageType.CON;
            } else {
                messageType = MessageType.NON;
            }

            //Create CoAP request to discover resources
            URI targetURI = new URI("coap", null, serverName, portNumber, "/.well-known/core", null, null);
            CoapRequest coapRequest = new CoapRequest(messageType, MessageCode.GET, targetURI);

            //Set block2 option (if any)
            if(BlockSize.UNBOUND != this.block2Size) {
                coapRequest.setPreferredBlock2Size(this.block2Size);
            }

            this.coapClient.sendCoapRequest(coapRequest, remoteEndpoint, new ServiceDiscoveryCallback());

            return null;

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


    private class ServiceDiscoveryCallback extends ClientCallback {

        @Override
        public void processCoapResponse(final CoapResponse coapResponse) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();

                    try {
                        String payload = coapResponse.getContent().toString(CoapMessage.CHARSET);
                        LinkValueList linkValueList = LinkValueList.decode(payload);

                        String[] uriReferences = new String[linkValueList.getUriReferences().size()];
                        uriReferences = linkValueList.getUriReferences().toArray(uriReferences);
                        Arrays.sort(uriReferences);

                        showToast("Found " + uriReferences.length + " Resources!");

//                        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
//                                android.R.layout.simple_spinner_dropdown_item, uriReferences);

                        ArrayAdapter adapter = new ArrayAdapter(activity, R.layout.spinner_item, uriReferences);

//                        adapter.setDropDownViewResource(R.layout.spinner_item);
//                        ((Spinner) activity.findViewById(R.id.txt_service)).setAdapter(adapter);
//                        ((Spinner) view.findViewById(R.id.spn_block1)).setAdapter(adapter);

                        AutoCompleteTextView txtService =
                                ((AutoCompleteTextView) activity.findViewById(R.id.txt_service));
                        txtService.setAdapter(adapter);
                        txtService.setHint(R.string.service_hint2);

                        //Set Method Spinner to GET
                        ((Spinner) activity.findViewById(R.id.spn_methods)).setSelection(1);
                    } catch (Exception ex) {
                        showToast("Unexpected ERROR:\n" + ex.getMessage());
                    }
                }
            });
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
        public void processBlockwiseResponseTransferFailed() {
            showToast("Blockwise response transfer failed for some unknown reason...");
        }

        @Override
        public void processMiscellaneousError(final String description) {
            showToast("ERROR: " + description + "!");
        }

    }
}

