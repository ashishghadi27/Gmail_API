package com.ran.voicemailclient.Activities.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ran.voicemailclient.Activities.helper.Speaker;
import com.ran.voicemailclient.R;

public class ViewMail extends Fragment {

    private TextView sender, date, time, message;
    private String senderstr, datestr, timestr, messagestr;
    private RelativeLayout mainlay;
    private final int CHECK_CODE = 0x1;
    private final int LONG_DURATION = 5000;
    private final int SHORT_DURATION = 1200;

    private Speaker speaker;

    public ViewMail() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        senderstr = args.getString("sender");
        datestr = args.getString("date");
        timestr = args.getString("time");
        messagestr = args.getString("message");
        checkTTS();
        speaker = new Speaker(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_view_mail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sender = view.findViewById(R.id.sender);
        date = view.findViewById(R.id.date);
        time = view.findViewById(R.id.time);
        message = view.findViewById(R.id.message);
        mainlay = view.findViewById(R.id.topBar);
        valueSetter();

        mainlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lets_speak();
            }
        });



    }

    private void checkTTS(){
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHECK_CODE){
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                speaker = new Speaker(getContext());
            }else {
                Intent install = new Intent();
                install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
            }
        }
    }

    public void lets_speak(){
        Log.v("SPEAK","LGLKJ");
        speaker.allow(true);
        speaker.speak("Reading mail from "+senderstr+". This mail was received on "+datestr+" at "+timestr+". Message is "+messagestr);


    }

    private void valueSetter(){
        sender.setText(senderstr);
        date.setText(datestr);
        time.setText(timestr);
        message.setText(messagestr);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        speaker.destroy();
    }
}
