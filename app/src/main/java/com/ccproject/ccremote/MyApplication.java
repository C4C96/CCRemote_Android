package com.ccproject.ccremote;

import android.app.Application;
import android.content.Context;

import com.ccproject.ccremote.baseComponent.LocalServer;

public class MyApplication extends Application
{
	private static Context mContext;

	public LocalServer mLocalServer;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mContext = getApplicationContext();
	}

	public static Context getContext()
	{
		return mContext;
	}
}
