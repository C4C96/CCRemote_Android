package com.ccproject.ccremote;

import android.app.Application;
import android.content.Context;

import com.ccproject.ccremote.baseComponent.LocalServer;

public class MyApplication extends Application
{
	private static MyApplication mMyApplication;

	public LocalServer mLocalServer;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mMyApplication = (MyApplication) getApplicationContext();
	}

	public static Context getContext()
	{
		return mMyApplication;
	}

	public static MyApplication getInstance()
	{
		return mMyApplication;
	}
}
