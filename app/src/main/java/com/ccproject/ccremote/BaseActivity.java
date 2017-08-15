package com.ccproject.ccremote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class BaseActivity extends AppCompatActivity
{
	protected final String TAG = getClass().getSimpleName();	//当前的activity名字

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}
}
