package com.ccproject.ccremote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;

import com.ccproject.ccremote.baseComponent.BaseActivity;
import com.ccproject.ccremote.baseComponent.LocalServer;
import com.ccproject.ccremote.explorer.ExplorerActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements LocalServer.ResponseHandle
{
	private NotificationManager mNotificationManager;
	private List<Integer> mLocalIdList; // 在本地显示的异步请求的id

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mLocalIdList = new ArrayList<>();

		Intent intent  = getIntent();
		String ip = intent.getStringExtra("ip");
		int port = intent.getIntExtra("port", -1);

		if (myApplication.mLocalServer != null)
			myApplication.mLocalServer.disconnect();
		myApplication.mLocalServer = new LocalServer(ip, port);
		myApplication.mLocalServer.setHeartBeatListener(this);

		findViewById(R.id.TEMP_BUTTON).setOnClickListener((v)->ExplorerActivity.actionStart(this));
	}

	@Override
	protected void onDestroy()
	{
		if (myApplication.mLocalServer != null)
			myApplication.mLocalServer.disconnect();
		mNotificationManager.cancelAll();
		super.onDestroy();
	}

	@Override
	public void handleResponse(byte[] response)
	{
		int cursor = 0;
		List<AsyncOperation> receivedList = new ArrayList<>();
		while (cursor < response.length)
		{
			int id = Tools.getInt(response, cursor);
			cursor += 4;
			int strLength = Tools.getInt(response, cursor);
			cursor += 4;
			String title = Tools.getString(response, cursor, strLength);
			cursor += strLength;
			long maxValue = Tools.getLong(response, cursor);
			cursor += 8;
			long value = Tools.getLong(response, cursor);
			cursor += 8;
			receivedList.add(new AsyncOperation(id, title, maxValue, value));
		}
		// 移除已消失的AO
		for (Integer localId : mLocalIdList)
		{
			boolean exist = false;
			for (AsyncOperation ao : receivedList)
				if (localId == ao.id)
				{
					exist = true;
					break;
				}
			if (!exist)
			{
				mNotificationManager.cancel(localId);
				mLocalIdList.remove(localId);
			}
		}
		for (AsyncOperation receive : receivedList)
			mNotificationManager.notify(receive.id,
					getNotification(receive.title, receive.value, receive.maxValue));
	}

	/**
	 *  生成显示异步操作进度的通知实例
	 * */
	private Notification getNotification(String title, long value, long maxValue)
	{
		int _value, _maxValue;
		if (maxValue > Integer.MAX_VALUE)
		{
			_maxValue = Integer.MAX_VALUE;
			_value = (int)(((float)value / (float)maxValue) * _maxValue);
		}
		else
		{
			_maxValue = (int)maxValue;
			_value = (int)value;
		}
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		return builder.setSmallIcon(R.mipmap.ic_launcher)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
				.setContentIntent(pi)
				.setContentTitle(title)
				.setContentText((float)value / (float)maxValue * 100 + "%")
				.setProgress(_maxValue, _value, false)
				.build();
	}

	public static void actionStart(Context context, String ip, int port)
	{
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra("ip", ip);
		intent.putExtra("port", port);
		context.startActivity(intent);
	}

	private class AsyncOperation
	{
		public int id;
		public String title;
		public long maxValue;
		public long value;

		public AsyncOperation(int id, String title, long maxValue, long value)
		{
			this.id = id;
			this.title = title;
			this.maxValue = maxValue;
			this.value = value;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof  AsyncOperation)
				return ((AsyncOperation)obj).id == this.id;
			return super.equals(obj);
		}
	}
}
