package com.ccproject.ccremote;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class MyPreferenceActivity extends BaseActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_preference);

		Toolbar toolbar = (Toolbar)findViewById(R.id.Preference_Toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		getFragmentManager().beginTransaction().replace(R.id.Preference_Fragment, new MyPreferenceFragment()).commit();
	}

}
