package de.uzl.itm.ncoap.android.client;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.net.URI;

import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageType;
import de.uniluebeck.itm.ncoap.message.options.OpaqueOptionValue;
import de.uniluebeck.itm.ncoap.message.options.UintOptionValue;
import de.uzl.itm.client.R;


public class ResponseFragment extends Fragment implements RadioGroup.OnCheckedChangeListener{

    private CoapResponse coapResponse;
    private URI serviceURI;

    private SendRequestTask.AndroidClientCallback clientCallback;

    public ResponseFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_response, container, false);

//        if (coapResponse != null && serviceURI != null) {
//            TextView txtResponse = (TextView) view.findViewById(R.id.txt_response_payload);
//            txtResponse.setText(coapResponse.getContent().toString(CoapMessage.CHARSET));
//
//            TextView txtServiceURI = (TextView) view.findViewById(R.id.txt_uri);
//            txtServiceURI.setText(serviceURI.toString());
//
//            TextView txtResponseCode = (TextView) view.findViewById(R.id.txt_code_response);
//
//            int messageCode = coapResponse.getMessageCode();
//            txtResponseCode.setText("Code: " + ((messageCode >>> 5) & 7) + "." +
//                    String.format("%02d", messageCode & 31));
//
//        }

        RadioGroup btnCancelObservation = (RadioGroup) view.findViewById(R.id.rad_stop_observation_group);
        btnCancelObservation.setOnCheckedChangeListener(this);
        return view;
    }


    public void setClientCallback(SendRequestTask.AndroidClientCallback clientCallback){
        if(this.clientCallback != null && isObservationRunning()){
            this.clientCallback.cancelObservation();
        }

        ((RadioGroup) getActivity().findViewById(R.id.rad_stop_observation_group)).clearCheck();

        RadioButton radStopObservation = (RadioButton) getActivity().findViewById(R.id.rad_stop_observation);
        radStopObservation.setEnabled(true);
        radStopObservation.setVisibility(View.VISIBLE);

        this.clientCallback = clientCallback;
    }


    public boolean isObservationRunning(){
        return !((RadioButton) getActivity().findViewById(R.id.rad_stop_observation)).isChecked();
    }

    public void responseReceived(URI serviceURI, CoapResponse coapResponse){

        this.serviceURI = serviceURI;
        this.coapResponse = coapResponse;

        TextView txtResponse = (TextView) getActivity().findViewById(R.id.txt_response_payload);
        txtResponse.setText(coapResponse.getContent().toString(CoapMessage.CHARSET));

        TextView txtServiceURI = (TextView) getActivity().findViewById(R.id.txt_uri);
        txtServiceURI.setText(serviceURI.toString());

        //Response Type
        TextView txtResponseType = (TextView) getActivity().findViewById(R.id.txt_type_response);
        MessageType.Name messageType = coapResponse.getMessageTypeName();
        if(messageType == MessageType.Name.CON){
            txtResponseType.setText("CON");
        }
        else if(messageType == MessageType.Name.NON){
            txtResponseType.setText("NON");
        }
        else if(messageType == MessageType.Name.ACK){
            txtResponseType.setText("ACK");
        }
        else{
            txtResponse.setText("UNKNOWN");
        }

        //ETAG Option
        byte[] etagValue = coapResponse.getEtag();
        if(etagValue != null) {
            TextView txtEtag = (TextView) getActivity().findViewById(R.id.txt_etag_response);
            txtEtag.setText(OpaqueOptionValue.toHexString(etagValue));
        }

        //Observe Option
        long observeValue = coapResponse.getObserve();
        if(observeValue != UintOptionValue.UNDEFINED){
            TextView txtObserve = (TextView) getActivity().findViewById(R.id.txt_observe_response);
            txtObserve.setText("" + observeValue);
        }

        //Content Format Option
        long contentFormatValue = coapResponse.getContentFormat();
        if(contentFormatValue != UintOptionValue.UNDEFINED){
            TextView txtContentFormat = (TextView) getActivity().findViewById(R.id.txt_contenttype_response);
            txtContentFormat.setText("" + contentFormatValue);
        }

        //Max-Age Option
        long maxageValue = coapResponse.getMaxAge();
        TextView txtMaxAge = (TextView) getActivity().findViewById(R.id.txt_maxage_response);
        txtMaxAge.setText("" + maxageValue);

        //Block2 Option
        long block2Number = coapResponse.getBlock2Number();
        if(block2Number != UintOptionValue.UNDEFINED){
            TextView txtBlock2 = (TextView) getActivity().findViewById(R.id.txt_block2_response);
            txtBlock2.setText("No: " + block2Number + " | SZX: " + coapResponse.getBlock2Szx());
        }

        //TODO: Size1 and Location-URI

        //Response Code
        TextView txtResponseCode = (TextView) getActivity().findViewById(R.id.txt_code_response);
        int messageCode = coapResponse.getMessageCode();
        txtResponseCode.setText("" + ((messageCode >>> 5) & 7) + "." + String.format("%02d", messageCode & 31));


        if(!coapResponse.isUpdateNotification()){
            RadioButton radStopObservation = (RadioButton) getActivity().findViewById(R.id.rad_stop_observation);
            radStopObservation.setChecked(true);
            radStopObservation.setEnabled(false);
            radStopObservation.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rad_stop_observation) {
            if(((RadioButton) getActivity().findViewById(R.id.rad_stop_observation)).isChecked()){
                this.clientCallback.cancelObservation();
            }
        }
    }
}
