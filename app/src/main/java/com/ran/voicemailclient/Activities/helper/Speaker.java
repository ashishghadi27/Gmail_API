package com.ran.voicemailclient.Activities.helper;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class Speaker implements TextToSpeech.OnInitListener{
    private TextToSpeech tts;

    private boolean ready = true;

    private boolean allowed = true;

    public Speaker(Context context){
        tts = new TextToSpeech(context, this);
    }

    public boolean isAllowed(){
        return allowed;
    }

    public void allow(boolean allowed){
        this.allowed = allowed;
    }

    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            tts.setLanguage(Locale.ROOT);
            Log.v("HERE", "true");
            ready = true;
        }else{
            ready = false;
        }
    }

    public void speak(String charSequence){

        int position = 0 ;
        int sizeOfChar= charSequence.length();
        String testStri= charSequence.substring(position,sizeOfChar);


        int next = 3500;
        int pos =0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(charSequence, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(charSequence, TextToSpeech.QUEUE_FLUSH, null);
        }
        while(true) {
            String temp="";
            Log.e("in loop", "" + pos);

            try {

                temp = testStri.substring(pos, next);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, temp);
                tts.speak(temp, TextToSpeech.QUEUE_ADD, params);

                pos = pos + 20;
                next = next + 20;

            } catch (Exception e) {
                temp = testStri.substring(pos, testStri.length());
                tts.speak(temp, TextToSpeech.QUEUE_ADD, null);
                break;

            }

        }

    }

    public void pause(int duration){
        tts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
    }

    public void destroy(){
        tts.shutdown();
    }
}
