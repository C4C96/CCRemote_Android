package com.ccproject.ccremote.preference;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.ccproject.ccremote.baseComponent.BaseActivity;
import com.ccproject.ccremote.R;

public class MyPreferenceActivity extends BaseActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_preference);

		Toolbar toolbar = (Toolbar)findViewById(R.id.Preference_Toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		getFragmentManager().beginTransaction().replace(R.id.Preference_Fragment, new MyPreferenceFragment()).commit();
	}

}
