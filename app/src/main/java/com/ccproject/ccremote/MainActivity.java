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
import java.lang.reflect.Array;
import java.util.Arrays;

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
					byte[] data = Arrays.copyOfRange(bytes, 4, bytes.length);
					String str = "";
					while (data.length > 8)
					{
						int length = (data[4] << 24)
									+ (data[5] << 16)
									+ (data[6] << 8)
									+ data[7];
						byte[] pathBytes = Arrays.copyOfRange(data, 8, length + 8);
						str += new String(pathBytes, "UTF-8");
						str += "\n";
						data = Arrays.copyOfRange(data, length + 8, data.length);
						receive.setText(str);
					}
				} catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}));
		((Button) findViewById(R.id.button_send)).setOnClickListener((view) ->
		{
			try
			{
				byte[] pathBytes = send.getText().toString().getBytes("UTF-8");
				byte[] sendBytes = new byte[pathBytes.length + 4];
				sendBytes[0] = 233 >> 24;
				sendBytes[1] = 233 >> 16;
				sendBytes[2] = 233 >> 8;
				sendBytes[3] = (byte)233;
				for(int i = 4; i < sendBytes.length; i++)
					sendBytes[i] = pathBytes[i - 4];
				mSocketUtil.send(sendBytes);
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
			mSocketUtil.disconnect();

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
