package com.ccproject.ccremote.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.ccproject.ccremote.MyApplication;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity
{
	protected final String TAG = getClass().getSimpleName();	//当前的activity名字

	protected MyApplication myApplication;
	private static List<BaseActivity> mActivityList = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		myApplication = (MyApplication) getApplication();
		mActivityList.add(this);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		mActivityList.remove(this);
	}

	public static void finishExcept(Class<?> type)
	{
		for (BaseActivity activity : mActivityList)
			if (type == null || !type.isInstance(activity))
				activity.finish();
	}

	public static BaseActivity find(Class<?> type)
	{
		for (BaseActivity activity : mActivityList)
			if (type.isInstance(activity))
				return activity;
		return null;
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
