package com.ccproject.ccremote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class MainActivity extends BaseActivity
{
	SocketUtil mSocketUtil = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TextView receive = (TextView) findViewById(R.id.main_receive);
		EditText send = (EditText) findViewById(R.id.main_send);

		Intent intent  = getIntent();
		String ip = intent.getStringExtra("ip");
		int port = intent.getIntExtra("port", -1);

		mSocketUtil = new SocketUtil(ip, port);
		mSocketUtil.connect();
		mSocketUtil.setOnDisconnectListener(socketUtil1 ->
			runOnUiThread(() ->
			{
				Log.d(TAG, "Disconnected.");
				Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
				MainActivity.this.finish();
			}));
		mSocketUtil.setOnMsgReceiveListener(bytes ->
			runOnUiThread(() ->
			{
				try
				{
					receive.setText("\""+new String(bytes, "UTF-8")+"\"");
				} catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}));
		((Button) findViewById(R.id.button_send)).setOnClickListener((view) ->
		{
			try
			{
				mSocketUtil.send(send.getText().toString().getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		});
	}

	@Override
	protected void onDestroy()
	{
		if (mSocketUtil != null)
			mSocketUtil.close();

		super.onDestroy();
	}

	public static void actionStart(Context context, String ip, int port)
	{
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra("ip", ip);
		intent.putExtra("port", port);
		context.startActivity(intent);
	}
}
