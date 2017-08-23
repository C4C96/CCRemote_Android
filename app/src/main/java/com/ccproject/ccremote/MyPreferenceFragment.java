package com.ccproject.ccremote;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class MyPreferenceFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_screen);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		switch (preference.getKey())
		{
			case "check_update":
				//TODO
				Toast.makeText(MyApplication.getContext(), "CheckUpdate", Toast.LENGTH_SHORT).show();
				break;
		}
		return true;
	}

	//TODO 若设置的port不符合范围，则不实行设置
}
