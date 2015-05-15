package de.uzl.itm.ncoap.android.client;

import android.app.Activity;
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

    private SendRequestTask.AndroidClientCallback clientCallback;
    private MainActivity mainActivity;
    
    public ResponseFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_response, container, false);

        this.mainActivity.setResponseFragment(this);

        RadioGroup btnCancelObservation = (RadioGroup) view.findViewById(R.id.rad_stop_observation_group);
        btnCancelObservation.setOnCheckedChangeListener(this);

        if(savedInstanceState != null) {
            CharSequence serviceURI = savedInstanceState.getCharSequence("uri");
            if (serviceURI != null) {
                ((TextView) view.findViewById(R.id.txt_uri)).setText(serviceURI);
            }

            CharSequence type = savedInstanceState.getCharSequence("type");
            if (type != null) {
                ((TextView) view.findViewById(R.id.txt_type_response)).setText(type);
            }

            CharSequence code = savedInstanceState.getCharSequence("code");
            if (code != null) {
                ((TextView) view.findViewById(R.id.txt_code_response)).setText(code);
            }

            CharSequence payload = savedInstanceState.getCharSequence("payload");
            if (payload != null) {
                ((TextView) view.findViewById(R.id.txt_response_payload)).setText(payload);
            }

            CharSequence etag = savedInstanceState.getCharSequence("etag");
            if (etag != null) {
                ((TextView) view.findViewById(R.id.txt_etag_response)).setText(etag);
            }

            CharSequence observe = savedInstanceState.getCharSequence("observe");
            if (observe != null) {
                ((TextView) view.findViewById(R.id.txt_observe_response)).setText(observe);
            }

            CharSequence contentformat = savedInstanceState.getCharSequence("contentformat");
            if (contentformat != null) {
                ((TextView) view.findViewById(R.id.txt_contenttype_response)).setText(contentformat);
            }

            CharSequence maxage = savedInstanceState.getCharSequence("maxage");
            if (maxage != null) {
                ((TextView) view.findViewById(R.id.txt_maxage_response)).setText(maxage);
            }

            CharSequence block2 = savedInstanceState.getCharSequence("block2");
            if (block2 != null) {
                ((TextView) view.findViewById(R.id.txt_block2_response)).setText(block2);
            }

            RadioButton radObservationCancelled = ((RadioButton) view.findViewById(R.id.rad_stop_observation));
            if(savedInstanceState.getBoolean("obs_cancelled")){
                radObservationCancelled.setChecked(true);
            }
            else{
                ((RadioGroup) view.findViewById(R.id.rad_stop_observation_group)).clearCheck();
            }
            radObservationCancelled.setEnabled(savedInstanceState.getBoolean("obs_enabled"));
            if(savedInstanceState.getInt("obs_visible") == View.INVISIBLE) {
                radObservationCancelled.setVisibility(View.INVISIBLE);
            }
        }

        return view;
    }


    @Override
    public void onSaveInstanceState(Bundle outState){
        //URI
        outState.putCharSequence("uri", getTextFromTextView(R.id.txt_uri));
        //Type
        outState.putCharSequence("type", getTextFromTextView(R.id.txt_type_response));
        //Code
        outState.putCharSequence("code", getTextFromTextView(R.id.txt_code_response));
        //Payload
        outState.putCharSequence("payload", getTextFromTextView(R.id.txt_response_payload));
        //ETAG
        outState.putCharSequence("etag", getTextFromTextView(R.id.txt_etag_response));
        //Observe
        outState.putCharSequence("observe", getTextFromTextView(R.id.txt_observe_response));
        //Content Format
        outState.putCharSequence("contentformat", getTextFromTextView(R.id.txt_contenttype_response));
        //Max-Age
        outState.putCharSequence("maxage", getTextFromTextView(R.id.txt_maxage_response));
        //ETAG
        outState.putCharSequence("block2", getTextFromTextView(R.id.txt_block2_response));
        //Observation stopped
        RadioButton radObservationCancelled = ((RadioButton) getActivity().findViewById(R.id.rad_stop_observation));
        outState.putBoolean("obs_cancelled", radObservationCancelled.isChecked());
        outState.putInt("obs_visible", radObservationCancelled.getVisibility());
        outState.putBoolean("obs_enabled", radObservationCancelled.isEnabled());
    }

    private CharSequence getTextFromTextView(int viewID){
        return ((TextView) getActivity().findViewById(viewID)).getText();
    }

    public void setClientCallback(MainActivity mainActivity, SendRequestTask.AndroidClientCallback clientCallback){
        if(this.clientCallback != null && isObservationRunning()){
            this.clientCallback.cancelObservation();
        }

        RadioGroup radioGroup = (RadioGroup) mainActivity.findViewById(R.id.rad_stop_observation_group);
        if(radioGroup != null) {
            radioGroup.clearCheck();

            RadioButton radStopObservation = (RadioButton) mainActivity.findViewById(R.id.rad_stop_observation);
            radStopObservation.setEnabled(true);
            radStopObservation.setVisibility(View.VISIBLE);
        }

        this.clientCallback = clientCallback;
    }


    public boolean isObservationRunning(){
        return !((RadioButton) getActivity().findViewById(R.id.rad_stop_observation)).isChecked();
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.mainActivity = (MainActivity) activity;
    }

    public void responseReceived(URI serviceURI, CoapResponse coapResponse){
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
            TextView txtEtag = (TextView) mainActivity.findViewById(R.id.txt_etag_response);
            txtEtag.setText(OpaqueOptionValue.toHexString(etagValue));
        }

        //Observe Option
        long observeValue = coapResponse.getObserve();
        if(observeValue != UintOptionValue.UNDEFINED){
            TextView txtObserve = (TextView) mainActivity.findViewById(R.id.txt_observe_response);
            txtObserve.setText("" + observeValue);
        }

        //Content Format Option
        long contentFormatValue = coapResponse.getContentFormat();
        if(contentFormatValue != UintOptionValue.UNDEFINED){
            TextView txtContentFormat = (TextView) mainActivity.findViewById(R.id.txt_contenttype_response);
            txtContentFormat.setText("" + contentFormatValue);
        }

        //Max-Age Option
        long maxageValue = coapResponse.getMaxAge();
        TextView txtMaxAge = (TextView) mainActivity.findViewById(R.id.txt_maxage_response);
        txtMaxAge.setText("" + maxageValue);

        //Block2 Option
        long block2Number = coapResponse.getBlock2Number();
        if(block2Number != UintOptionValue.UNDEFINED){
            TextView txtBlock2 = (TextView) mainActivity.findViewById(R.id.txt_block2_response);
            txtBlock2.setText("No: " + block2Number + " | SZX: " + coapResponse.getBlock2Szx());
        }

        //TODO: Size1 and Location-URI

        //Response Code
        TextView txtResponseCode = (TextView) mainActivity.findViewById(R.id.txt_code_response);
        int messageCode = coapResponse.getMessageCode();
        txtResponseCode.setText("" + ((messageCode >>> 5) & 7) + "." + String.format("%02d", messageCode & 31));


        if(!coapResponse.isUpdateNotification()){
            RadioButton radStopObservation = (RadioButton) mainActivity.findViewById(R.id.rad_stop_observation);
            radStopObservation.setChecked(true);
            radStopObservation.setEnabled(false);
            radStopObservation.setVisibility(View.INVISIBLE);
        }
    }

//    @Override
//    public void onAttach(Activity activity){
//        super.onAttach(activity);
//        this.mainActivity = (MainActivity) activity;
//    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rad_stop_observation && getActivity() != null) {
            if(this.clientCallback != null) {
                this.clientCallback.cancelObservation();
            }
        }
    }
}
