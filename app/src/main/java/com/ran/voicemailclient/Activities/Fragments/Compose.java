package com.ran.voicemailclient.Activities.Fragments;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.ran.voicemailclient.Activities.helper.InternetDetector;
import com.ran.voicemailclient.Activities.helper.Speaker;
import com.ran.voicemailclient.Activities.helper.Utils;
import com.ran.voicemailclient.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import static android.app.Activity.RESULT_OK;

public class Compose extends Fragment {

    private static final int REQ_CODE =100 ;
    EditText to, subject, attachment, message;
    public ImageView send;
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.MAIL_GOOGLE_COM
    };
    private LinearLayout layout;
    private InternetDetector internetDetector;
    private Speaker speaker;
    private int numberOfClicks;
    private boolean IsInitialVoiceFinshed;
    private TextToSpeech tts;

    public Compose() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speaker = new Speaker(getContext());
        numberOfClicks = 0;
        IsInitialVoiceFinshed = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        to = view.findViewById(R.id.receiver);
        subject = view.findViewById(R.id.subject);
        attachment = view.findViewById(R.id.attachment);
        message = view.findViewById(R.id.message);
        send = view.findViewById(R.id.send);
        init();

        layout=view.findViewById(R.id.lay);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IsInitialVoiceFinshed) {
                    numberOfClicks++;
                    listen();
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getResultsFromApi(view);
            }
        });

        tts = new TextToSpeech(this.getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    speak("Tell me the mail address to whom you want to send mail?");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            IsInitialVoiceFinshed = true;
                        }
                    }, 6000);
                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });
    }

    private void init() {
        // Initializing Internet Checker
        internetDetector = new InternetDetector(getContext());

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        // Initializing Progress Dialog
        mProgress = new ProgressDialog(getContext());
        mProgress.setMessage("Sending...");


    }
    private void speak(String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void showMessage(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    private void getResultsFromApi(View view) {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount(view);
        } else if (!internetDetector.checkMobileInternetConn()) {
            speak("No network connection available.");
        } else if (!Utils.isNotEmpty(to)) {
            speak("To address Required");
        } else if (!Utils.isNotEmpty(subject)) {
            speak("Subject Required");
        } else if (!Utils.isNotEmpty(message)) {
            speak("Message Required");
        } else {
            new MakeRequestTask(getContext(), mCredential).execute();
        }
    }

    private void listen(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
        try {
            startActivityForResult(intent, REQ_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getContext(),
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }
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
    private void chooseAccount(View view) {
        if (Utils.checkPermission(getContext(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getActivity().getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi(view);
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), Utils.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.GET_ACCOUNTS}, Utils.REQUEST_PERMISSION_GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.REQUEST_PERMISSION_GET_ACCOUNTS:
                chooseAccount(send);
                break;

        }
    }
    private void addFragment(Fragment fragment, String tag){
        Objects.requireNonNull(getActivity()).getSupportFragmentManager()
                .beginTransaction().add(R.id.fragment_container, fragment, tag).addToBackStack(tag)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utils.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    showMessage(send, "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi(send);
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
                        getResultsFromApi(send);
                    }
                }
                break;
            case Utils.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi(send);
                }
                break;
        }
            if(requestCode == 100&& IsInitialVoiceFinshed){
                IsInitialVoiceFinshed = false;
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    if(result.get(0).equals("cancel"))
                    {
                        speak("Cancelled!");
                    }
                    else {

                        switch (numberOfClicks) {
                            case 1:
                                String tom;

                                tom = result.get(0).replaceAll("underscore", "_");
                                tom= tom.replaceAll("\\s+", "");
                                tom = tom.toLowerCase() + "@gmail.com";
                                to.setText(tom);
                                speak("What should be the subject?");
                                break;
                            case 2:
                                subject.setText(result.get(0));
                                speak("Do you have any file attachments?");
                                break;

                            case 3:
                                if(result.get(0).equalsIgnoreCase("yes")) {
                                    speak("Tell me the file name");
                                    break;
                                }
                                else{
                                    numberOfClicks++;
                                    speak("Give me message");
                                }

                                break;
                            case 4:

                                if(result.get(0) != null)
                                    attachment.setText(Environment.getExternalStorageDirectory() + File.separator + result.get(0));
                                else speak("Failed to attach file");
                                speak("Give me message");
                                break;

                        case 5 :
                            message.setText(result.get(0));
                            speak("Do you want to send the mail? If yes, say send mail else say cancel");
                            break;
                      /*  case 5 :
                            Config.PASSWORD =result.get(0);
                            status.setText("Confirm?");
                            speak("Please Confirm the mail\n To : " + To.getText().toString() + "\nSubject : " + Subject.getText().toString() + "\nMessage : " + Message.getText().toString() +"your mail "+Config.EMAIL+"your password" +Config.PASSWORD + "\nSpeak Yes to confirm");
                            break;
*/
                        /*    case 4:
                                yesno.setText(result.get(0));
                                if (yesno.getText().equals("yes")) {
                                    speak("Tell the file name");
                                    edtAttachmentData.setText(result.get(0));
                                    // speak("sending mail");
                                    Intent intent = new Intent(Intent.ACTION_SEND);


// Always use string resources for UI text.
// This says something like "Share this photo with"
                                    String title = getResources().getString(R.string.chooser_title);
// Create intent to show chooser
                                    Intent chooser = Intent.createChooser(intent, title);

// Verify the intent will resolve to at least one activity
                                    if (intent.resolveActivity(getPackageManager()) != null) {
                                        startActivity(chooser);
                                    }

                                    break;

                                }else if (yesno.getText().equals("no")) {
                                    // speak("Sending the mail without attachment");
                                    break;
                                }
                     /*  case 5:
                               edtAttachmentData.setText(result.get(0));
                               speak("Sending the mail");*/
                                // case 5:
                           /* if (yesno.getText().equals("yes")) {
                                edtAttachmentData.setText(result.get(0));
                     //           speak("sending mail");


                                //  speak("Sending the mail without attachment");
*/

                            case 6:
                                if(result.get(0).equalsIgnoreCase("send mail"))
                                {
                                    send.performClick();
                                    speak("mail sent");
                                    break;
                                }
                                else
                                    if(result.get(0).equalsIgnoreCase("cancel"))
                                    {
                                        addFragment(new Choice(),"Choice");
                                        speak("cancelled sending the mail");
                                    }
                                Log.v("CHECK", numberOfClicks+"");
                                    break;
                        }
                    }
                }
                else {
                   switch (numberOfClicks) {
                        case 1:
                            speak(" whom you want to send mail?");
                            break;
                        case 2:
                            speak("What should be the subject?");
                            break;
                        case 3:
                            speak("Give me message");
                            break;
                        case 4:
                            speak("Do you have any file attachments");
                            break;
                        case 5:
                            speak("send the mail?");
                            break;

                    }
                    numberOfClicks--;
                }
            }
            IsInitialVoiceFinshed=true;

        }



    // Async Task for sending Mail using GMail OAuth
    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private Gmail mService = null;
        private Exception mLastError = null;
        private View view = send;
        private Context activity;

        MakeRequestTask(Context activity, GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getResources().getString(R.string.app_name))
                    .build();
            this.activity = activity;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private String getDataFromApi() throws IOException {
            // getting Values for to Address, from Address, Subject and Body
            String user = "me";
            String tostr = Utils.getString(to);
            String from = mCredential.getSelectedAccountName();
            String subjectstr = Utils.getString(subject);
            String body = Utils.getString(message);
            MimeMessage mimeMessage;
            String response = "";
            try {
                mimeMessage = createEmail(tostr, from, subjectstr, body);
                response = sendMessage(mService, user, mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return response;
        }

        // Method to send email
        private String sendMessage(Gmail service,
                                   String userId,
                                   MimeMessage email)
                throws MessagingException, IOException {
            Message message = createMessageWithEmail(email);
            // GMail's official method to send email with oauth2.0
            message = service.users().messages().send(userId, message).execute();

            System.out.println("Message id: " + message.getId());
            System.out.println(message.toPrettyString());
            return message.getId();
        }

        // Method to create email Params
        private MimeMessage createEmail(String to,
                                        String from,
                                        String subject,
                                        String bodyText) throws MessagingException {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            InternetAddress tAddress = new InternetAddress(to);
            InternetAddress fAddress = new InternetAddress(from);

            email.setFrom(fAddress);
            email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
            email.setSubject(subject);

            // Create Multipart object and add MimeBodyPart objects to this object
            Multipart multipart = new MimeMultipart();

            // Changed for adding attachment and text
            // email.setText(bodyText);

            BodyPart textBody = new MimeBodyPart();
            textBody.setText(bodyText);
            multipart.addBodyPart(textBody);

            String fileName = attachment.getText()+"";

            if (!(fileName.equals(""))) {
                // Create new MimeBodyPart object and set DataHandler object to this object
                MimeBodyPart attachmentBody = new MimeBodyPart();

                    File file = new File(fileName);
                    Log.v("FILE", file.exists() + " dc");


                DataSource source = new FileDataSource(file);
                Log.v("FILE", "Below ds");
                attachmentBody.setDataHandler(new DataHandler(source));
                Log.v("FILE", "Below Handler");
                attachmentBody.setFileName(file.getName());
                Log.v("FILE", "Below setname");
                multipart.addBodyPart(attachmentBody);
                Log.v("FILE", "Below attach");
            }

            //Set the multipart object to the message object
            email.setContent(multipart);
            return email;
        }

        private Message createMessageWithEmail(MimeMessage email)
                throws MessagingException, IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            email.writeTo(bytes);
            String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
            Message message = new Message();
            message.setRaw(encodedEmail);
            return message;
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            if (output == null || output.length() == 0) {
                showMessage(view, "No results returned.");
            } else {
                showMessage(view, output);
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
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
                    showMessage(view, "The following error occurred:\n" + mLastError);
                    Log.v("Error", mLastError + "");
                }
            } else {
                showMessage(view, "Request Cancelled.");
            }
        }
    }
}
