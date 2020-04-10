package com.ran.voicemailclient.Activities.Fragments;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.ran.voicemailclient.Activities.Adapters.LoadingAdapter;
import com.ran.voicemailclient.Activities.Interface.FragmentAddingInterface;
import com.ran.voicemailclient.Activities.Models.DisplayMessage;
import com.ran.voicemailclient.Activities.helper.Utils;
import com.ran.voicemailclient.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class Read extends Fragment{

    private RecyclerView recyclerView;

    private GoogleAccountCredential mCredential;
    private ShimmerFrameLayout container;
    private Gmail mService;
    private static final String[] SCOPES = {
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.MAIL_GOOGLE_COM
    };

    private String account = "";
    private static final String PREF_ACCOUNT_NAME = "accountName";

    public Read() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        container = (ShimmerFrameLayout) view.findViewById(R.id.shimmer_view_container);
        container.setVisibility(View.VISIBLE);
        container.startShimmerAnimation();
        init();
    }

    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
        else new MakeRequestTask(mCredential, getContext()).execute();

    }

    // Method for Checking Google Play Service is Available
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    // Method to Show Info, If Google Play Service is Not Available.
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    // Method for Google Play Services Error Info
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                Utils.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    // Storing Mail ID using Shared Preferences
    private void chooseAccount() {
        if (Utils.checkPermission(getContext(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getActivity().getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), Utils.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                    new String[]{Manifest.permission.GET_ACCOUNTS}, Utils.REQUEST_PERMISSION_GET_ACCOUNTS);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utils.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                } else {
                    getResultsFromApi();
                }
                break;
            case Utils.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {

                        SharedPreferences settings = getActivity().getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case Utils.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;

        }
    }



    private void init(){
        SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("data",Context.MODE_PRIVATE);;
        account = settings.getString(PREF_ACCOUNT_NAME, "me");
        Log.v("ACCOUNT", account + "bm");
        Toast.makeText(getContext(), account + "  sdc", Toast.LENGTH_SHORT).show();

        mCredential = GoogleAccountCredential.usingOAuth2(
                getContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(account);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();
                getResultsFromApi();
        //task.doInBackground();
    }



    private class MakeRequestTask extends AsyncTask<Void, Void, List<DisplayMessage>> implements FragmentAddingInterface {

        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private Context context;
        private List<DisplayMessage> displayMessageList;
        private LoadingAdapter loadingAdapter;


        MakeRequestTask(GoogleAccountCredential credential, Context context) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getResources().getString(R.string.app_name))
                    .build();
            this.context = context;
            displayMessageList = new ArrayList<>();
            loadingAdapter = new LoadingAdapter(displayMessageList, getContext(), this);
            recyclerView.setAdapter(loadingAdapter);

        }

        @Override
        protected List<DisplayMessage> doInBackground(Void... params) {
            try {
                return listMessagesMatchingQuery(mService, "me", "");
            } catch (Exception e) {
                mLastError = e;
                Log.v("RESP", e.getMessage()+ "ngc");
                cancel(true);
                return null;
            }

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(List<DisplayMessage> output) {


            loadingAdapter.notifyDataSetChanged();
            container.setVisibility(View.GONE);
            container.stopShimmerAnimation();

        }

        @Override
        protected void onCancelled() {

            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Utils.REQUEST_AUTHORIZATION);
                } else {
                    //showMessage(view, "The following error occurred:\n" + mLastError);
                    Log.v("Error", mLastError.getCause() + "");
                }
            } else {
                //showMessage(view, "Request Cancelled.");
            }

        }

        @Override
        public void addFragment(Fragment fragment, String tag, Bundle args) {
            fragment.setArguments(args);
            Objects.requireNonNull(getActivity()).getSupportFragmentManager()
                    .beginTransaction().add(R.id.fragment_container, fragment, tag).addToBackStack(tag)
                    .commit();
        }

        private List<DisplayMessage> listMessagesMatchingQuery(Gmail service, String userId,
                                                               String query) throws IOException {
            Log.v("RESP", "jdshb");
            List<String> list = new ArrayList<>();
            list.add("CATEGORY_PERSONAL");
            list.add("INBOX");
            long max = 1;
            ListMessagesResponse response = service.users().messages().list(userId).setLabelIds(list).execute();
            Log.v("RESP", response.getMessages().toString() + "jdshb");
            List<Message> messages = new ArrayList<Message>();
            while (response.getMessages() != null && max!=0) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(userId)
                            .setPageToken(pageToken).execute();
                    max--;
                    Log.v("NEW RESP", response.toString());
                } else {
                    break;
                }
            }

            int count = 20;

            for (Message message : messages) {
                if(count < 1){
                    break;
                }
                Message messagenew = service.users().messages().get("me", message.getId()).setFormat("full").execute();
                //Log.v("HEADER", messagenew.getPayload().toPrettyString()+ "jhhjb");
                Log.v("MESSAGE", messagenew.getPayload().getParts() + "jhhjb");
                try {
                    JSONObject jsonObject = new JSONObject(messagenew.getPayload().toPrettyString());
                    JSONArray jsonArray = jsonObject.getJSONArray("headers");
                    JSONObject receiver = jsonArray.getJSONObject(0);
                    JSONObject receivingTime = jsonArray.getJSONObject(1);
                    JSONObject sender = jsonArray.getJSONObject(6);

                    Log.v("JSON ARRAY", messagenew.getPayload().getParts()+ "dcs");

                    String senderstr = sender.getString("value");
                    senderstr = senderstr.replace("<","");
                    senderstr = senderstr.replace(">","");
                    String receivingTimestr = receivingTime.getString("value");
                    String arr[] = receivingTimestr.split(";");

                    String messagePart = "";
                    try{
                        JSONArray jsonArray1 = new JSONArray(messagenew.getPayload().getParts()).getJSONObject(0).getJSONArray("parts");

                        for(int i = 0; i < jsonArray1.length(); i++){
                            try {
                                Log.v("ARE BABA", jsonArray1.toString());
                                String mess = jsonArray1.getJSONObject(0).getJSONObject("body").getString("data");
                                mess = mess.replace("-","+");
                                mess = mess.replace("_","/");
                                Log.v("MESS", mess);
                                messagePart = messagePart + new String(Base64.decode(mess, android.util.Base64.DEFAULT));
                            }
                            catch (Exception e){
                                Log.v("EXCEPTION", e.getMessage()+"");
                            }
                        }

                    }catch (JSONException e){
                        try{
                            JSONArray jsonArray1 = new JSONArray(messagenew.getPayload().getParts());
                            String mess = jsonArray1.getJSONObject(0).getJSONObject("body").getString("data");
                            mess = mess.replace("-","+");
                            mess = mess.replace("_","/");
                            Log.v("MESS NEXT", mess);
                            messagePart = new String(Base64.decode(mess,android.util.Base64.DEFAULT));
                        }catch (Exception x){
                            Log.v("EXCEPTION", e.getMessage()+"");
                        }
                    }
                    finally {
                        DisplayMessage displayMessage = new DisplayMessage(senderstr, arr[1].trim(), messagePart);
                        displayMessageList.add(displayMessage);
                        count--;
                    }


                } catch (JSONException e) {
                    Log.v("JSON", e.getCause() + "jhhjb");
                    e.printStackTrace();
                }


            }


        /*ListLabelsResponse response = service.users().labels().list(userId).execute();
        List<Label> labels = response.getLabels();
        for (Label label : labels) {
            Log.v("LABEL",label.toPrettyString());
        }*/


            return displayMessageList;
        }
    }






}




