package com.ccproject.ccremote;

import android.app.Application;
import android.content.Context;

import java.net.Socket;

public class MyApplication extends Application
{
	private static Context context;

	public Socket socket;

	@Override
	public void onCreate()
	{
		super.onCreate();
		context = getApplicationContext();
	}

	public static Context getContext()
	{
		return context;
	}
}
