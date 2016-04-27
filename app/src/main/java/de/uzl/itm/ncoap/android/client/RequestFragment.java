package de.uzl.itm.ncoap.android.client;

import android.app.Activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;

import android.widget.Spinner;
import de.uzl.itm.client.R;


/**
 * A placeholder fragment containing a simple view.
 */
public class RequestFragment extends Fragment implements View.OnClickListener{

    private SendRequestButtonClickedListener sendRequestButtonClickedListener;


    public RequestFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        //register this fragment as listener to click events on the send button
        view.findViewById(R.id.btn_send).setOnClickListener(this);

        //restore previously discovered services
        if(savedInstanceState != null) {
            String[] serviceNames = savedInstanceState.getStringArray("services");
            if (serviceNames != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_spinner_dropdown_item, serviceNames);

                AutoCompleteTextView txtService = ((AutoCompleteTextView) view.findViewById(R.id.txt_service));
                txtService.setAdapter(adapter);
                txtService.setHint(R.string.service_hint2);
            }
        }

        ArrayAdapter adapter = ArrayAdapter.createFromResource(this.getContext(), R.array.blocksizes,
                R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        ((Spinner) view.findViewById(R.id.spn_block2)).setAdapter(adapter);
        ((Spinner) view.findViewById(R.id.spn_block1)).setAdapter(adapter);

        return view;
    }




    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_send){
            sendRequestButtonClickedListener.onSendButtonClicked();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        //Store actual data from service adapter
        View serviceView = getActivity().findViewById(R.id.txt_service);
        if(serviceView != null) {
            ListAdapter serviceAdapter = ((AutoCompleteTextView) serviceView).getAdapter();
            if (serviceAdapter != null) {
                int count = serviceAdapter.getCount();
                String[] services = new String[count];
                for (int i = 0; i < count; i++) {
                    services[i] = (String) serviceAdapter.getItem(i);
                }
                outState.putCharSequenceArray("services", services);
            }
        }
    }


    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.sendRequestButtonClickedListener = (SendRequestButtonClickedListener) activity;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        this.sendRequestButtonClickedListener = null;
    }

    /**
     * Interface to be implemented by the activity in order to react on click events
     */
    public interface SendRequestButtonClickedListener {

        void onSendButtonClicked();

    }
}
