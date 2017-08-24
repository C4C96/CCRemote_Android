package com.ccproject.ccremote;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class MyPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener
{
	private static final String PORT_KEY = "port";
	private static final String CHECK_UPDATE_KEY = "check_update";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_screen);

		EditTextPreference port = (EditTextPreference) findPreference(PORT_KEY);
		Preference checkUpdate = findPreference(CHECK_UPDATE_KEY);
		port.setOnPreferenceChangeListener(this);
		checkUpdate.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		switch (preference.getKey())
		{
			case PORT_KEY:
				int newPort = Integer.valueOf((String)newValue);
				if (newPort > 65535 || newPort < 1024)
				{
					Toast.makeText(MyApplication.getContext(), getResources().getString(R.string.PortRangeError), Toast.LENGTH_SHORT).show();
					return false;
				}
				break;
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		switch (preference.getKey())
		{
			case CHECK_UPDATE_KEY:
				//TODO 检查更新
				Toast.makeText(MyApplication.getContext(), "CheckUpdate", Toast.LENGTH_SHORT).show();
				break;
			default:
				return false;
		}
		return true;
	}
}
