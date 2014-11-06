package com.msevgi.memeex;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment {
	
	private static final String TAG = SettingsFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
		Log.d(TAG, "Starting preferences fragment");
        addPreferencesFromResource(R.xml.pref_general);

	}

	
}
