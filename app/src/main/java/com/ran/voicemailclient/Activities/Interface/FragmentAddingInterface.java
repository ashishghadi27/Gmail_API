package com.ran.voicemailclient.Activities.Interface;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

public interface FragmentAddingInterface {

    void addFragment(Fragment fragment, String tag, Bundle args);
}
