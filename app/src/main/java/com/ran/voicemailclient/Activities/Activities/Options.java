package com.ran.voicemailclient.Activities.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.ran.voicemailclient.Activities.Fragments.Choice;
import com.ran.voicemailclient.Activities.Interface.FragmentAddingInterface;
import com.ran.voicemailclient.R;

public class Options extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        addFragment(new Choice(), "Choice");
    }



    private void addFragment(Fragment fragment, String tag){
        getSupportFragmentManager()
                .beginTransaction().add(R.id.fragment_container, fragment, tag).addToBackStack(tag)
                .commit();
    }

}
