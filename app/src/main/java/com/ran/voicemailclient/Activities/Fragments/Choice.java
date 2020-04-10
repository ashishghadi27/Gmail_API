package com.ran.voicemailclient.Activities.Fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ran.voicemailclient.R;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class Choice extends Fragment {

    private ImageView layout;
    private boolean IsInitialVoiceFinshed;

    private final int REQ_CODE = 100;
    private TextToSpeech tts;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_choice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tts = new TextToSpeech(this.getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    speak("Welcome to Mailprop, Your personalised Voice based Mailing Application Lets begin! Select your choice: Say compose to compose the mail else Say Read to read the mails");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            IsInitialVoiceFinshed = true;
                        }
                    }, 3000);
                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });

        layout = view.findViewById(R.id.layout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speechToText();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                nextFragment(result.get(0));
            }

        }
    }
    private void speak(String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    private void speechToText(){
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

    private void nextFragment(String choice){
        if(choice.equalsIgnoreCase("compose")){
            addFragment(new Compose(),"Compose");
        }
        else if(choice.equalsIgnoreCase("read")){
            addFragment(new Read(),"Read");
        }
    }

    private void addFragment(Fragment fragment, String tag){
        Objects.requireNonNull(getActivity()).getSupportFragmentManager()
                .beginTransaction().add(R.id.fragment_container, fragment, tag).addToBackStack(tag)
                .commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        tts.shutdown();
    }
}
