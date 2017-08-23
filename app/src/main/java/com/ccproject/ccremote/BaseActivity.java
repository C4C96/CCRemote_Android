package com.ccproject.ccremote;

import android.app.Application;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

public class BaseActivity extends AppCompatActivity
{
	protected final String TAG = getClass().getSimpleName();	//当前的activity名字

	protected MyApplication myApplication;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		myApplication = (MyApplication) getApplication();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				finish();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}
}
