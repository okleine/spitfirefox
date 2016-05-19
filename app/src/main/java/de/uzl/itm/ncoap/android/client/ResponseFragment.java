package de.uzl.itm.ncoap.android.client;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.net.URI;
import java.net.URISyntaxException;

import de.uzl.itm.client.R;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.OpaqueOptionValue;
import de.uzl.itm.ncoap.message.options.UintOptionValue;


public class ResponseFragment extends Fragment implements RadioGroup.OnCheckedChangeListener{

    private SendRequestTask.SpitfirefoxCallback clientCallback;
    private CoapClientActivity clientActivity;
    
    public ResponseFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_response, container, false);

        this.clientActivity.setResponseFragment(this);

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
                ((TextView) view.findViewById(R.id.txt_max_age_response)).setText(maxage);
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
        outState.putCharSequence("maxage", getTextFromTextView(R.id.txt_max_age_response));
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

    public void setClientCallback(CoapClientActivity coapClientActivity, SendRequestTask.SpitfirefoxCallback clientCallback){
        if(this.clientCallback != null && isObservationRunning()){
            this.clientCallback.cancelObservation();
        }

        RadioGroup radioGroup = (RadioGroup) coapClientActivity.findViewById(R.id.rad_stop_observation_group);
        if(radioGroup != null) {
            radioGroup.clearCheck();

            RadioButton radStopObservation = (RadioButton) coapClientActivity.findViewById(R.id.rad_stop_observation);
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
        this.clientActivity = (CoapClientActivity) activity;
    }

    public void responseReceived(URI uri, CoapResponse coapResponse){
        TextView txtResponse = (TextView) getActivity().findViewById(R.id.txt_response_payload);
        txtResponse.setText("");
        txtResponse.setText(coapResponse.getContent().toString(CoapMessage.CHARSET));

        TextView txtURI = (TextView) getActivity().findViewById(R.id.txt_uri);
        txtURI.setText(uri.toString());

        //Response Type
        TextView txtResponseType = (TextView) getActivity().findViewById(R.id.txt_type_response);
        txtResponseType.setText(coapResponse.getMessageTypeName());

        //ETAG Option
        TableRow etagRow = (TableRow) clientActivity.findViewById(R.id.tabrow_etag_response);
        byte[] etagValue = coapResponse.getEtag();
        if(etagValue != null) {
            etagRow.setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_etag_response)).setText(
                    OpaqueOptionValue.toHexString(etagValue)
            );
        } else {
            etagRow.setVisibility(View.GONE);
        }

        //Observe Option
        TableRow observeRow = (TableRow) clientActivity.findViewById(R.id.tabrow_observe_response);
        long observeValue = coapResponse.getObserve();
        if(observeValue != UintOptionValue.UNDEFINED){
            observeRow.setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_observe_response)).setText("" + observeValue);
        } else {
            observeRow.setVisibility(View.GONE);
        }

        //Location-URI Options
        try {
            URI locationURI = coapResponse.getLocationURI();
            TableRow locationPathRow = (TableRow) clientActivity.findViewById(R.id.tabrow_location_path_response);
            TableRow locationQueryRow = (TableRow) clientActivity.findViewById(R.id.tabrow_location_query_response);

            if(locationURI != null) {
                //Location-Path Option
                String locationPath = locationURI.getPath();
                if(locationPath != null) {
                    locationPathRow.setVisibility(View.VISIBLE);
                    ((TextView) clientActivity.findViewById(R.id.txt_location_path_response)).setText(locationPath);
                } else {
                    locationPathRow.setVisibility(View.GONE);
                }

                //Location-Query Option
                String locationQuery = locationURI.getQuery();
                if(locationQuery != null) {
                    locationQueryRow.setVisibility(View.VISIBLE);
                    ((TextView) clientActivity.findViewById(R.id.txt_location_query_response)).setText(locationQuery);
                } else {
                    locationQueryRow.setVisibility(View.GONE);
                }
            } else {
                locationPathRow.setVisibility(View.GONE);
                locationQueryRow.setVisibility(View.GONE);
            }
        } catch(URISyntaxException ex) {
            String message = "ERROR (Malformed 'Location' Options): " + ex.getMessage();
            Toast.makeText(this.clientActivity, message, Toast.LENGTH_LONG).show();
        }

        //Content Format Option
        TableRow contentFormatRow = (TableRow) clientActivity.findViewById(R.id.tabrow_content_format_response);
        long contentFormatValue = coapResponse.getContentFormat();
        if(contentFormatValue != UintOptionValue.UNDEFINED){
            contentFormatRow.setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_contenttype_response)).setText("" + contentFormatValue);
        } else {
            contentFormatRow.setVisibility(View.GONE);
        }

        //Max-Age Option
        long maxAgeValue = coapResponse.getMaxAge();
        ((TextView) clientActivity.findViewById(R.id.txt_max_age_response)).setText("" + maxAgeValue);

        //Block2 Option
        long block2Number = coapResponse.getBlock2Number();
        if(block2Number != UintOptionValue.UNDEFINED){
            clientActivity.findViewById(R.id.tabrow_block2_response).setVisibility(View.VISIBLE);
            ((TextView) clientActivity.findViewById(R.id.txt_block2_response)).setText(
                    "No: " + block2Number + " | SZX: " + coapResponse.getBlock2Szx()
            );
        } else {
            clientActivity.findViewById(R.id.tabrow_block2_response).setVisibility(View.GONE);
        }

        //TODO: Size1 Option
        clientActivity.findViewById(R.id.tabrow_size1_response).setVisibility(View.GONE);


        //Response Code
        TextView txtResponseCode = (TextView) clientActivity.findViewById(R.id.txt_code_response);
        int messageCode = coapResponse.getMessageCode();
        txtResponseCode.setText("" + ((messageCode >>> 5) & 7) + "." + String.format("%02d", messageCode & 31));


        if(!coapResponse.isUpdateNotification()){
            RadioButton radStopObservation = (RadioButton) clientActivity.findViewById(R.id.rad_stop_observation);
            radStopObservation.setChecked(true);
            radStopObservation.setEnabled(false);
            radStopObservation.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rad_stop_observation && getActivity() != null) {
            if(this.clientCallback != null) {
                this.clientCallback.cancelObservation();
//                ((RadioButton) group.findViewById(R.id.rad_stop_observation)).setText(R.string.observation_stopped);
            }
        }
    }
}
