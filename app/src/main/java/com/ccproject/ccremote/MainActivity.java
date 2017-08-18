package com.ccproject.ccremote;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class MainActivity extends BaseActivity
{
	Socket socket = null;
	InputStream in;
	OutputStream out;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TextView receive = (TextView) findViewById(R.id.main_receive);
		EditText send = (EditText) findViewById(R.id.main_send);

		SocketUtil socketUtil = new SocketUtil("192.168.2.110", 2333);
		socketUtil.setOnConnectFailedListener(socketUtil1 ->
		{
			runOnUiThread(()-> Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show());
		});
		socketUtil.setOnMsgReceiveListener(bytes ->
		{
			runOnUiThread(() -> {
				try
				{
					receive.setText(new String(bytes, "UTF-8"));
				} catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			});
		});
		((Button)findViewById(R.id.button_connect)).setOnClickListener((view)->
		{
			socketUtil.connect();
		});
		((Button)findViewById(R.id.button_send)).setOnClickListener((view)->
		{
			try
			{
				socketUtil.send(send.getText().toString().getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		});

/*		Thread receiveThread = new Thread(()->
		{
			while(true)
			{
				if (socket == null) continue;
				if (socket.isClosed()) break;

				try
				{
					byte[] buffer = new byte[1024];
					int length = in.read(buffer);
					runOnUiThread(() -> {
						try
						{
							receive.setText(new String(buffer, 0, length, "UTF-8"));
						}
						catch (UnsupportedEncodingException e)
						{
							e.printStackTrace();
						}
					});
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		((Button)findViewById(R.id.button_connect)).setOnClickListener((view)->
		{
			new Thread(()->
			{
				try
				{
					socket = new Socket("192.168.2.110", 2333);
					in = socket.getInputStream();
					out = socket.getOutputStream();
					receiveThread.start();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
				}
			}).start();
		});
		((Button)findViewById(R.id.button_send)).setOnClickListener((view)->
		{
			if (socket == null) return;
			new Thread(()->
			{
				try
				{
					out.write(send.getText().toString().getBytes("UTF-8"));
					out.flush();
					runOnUiThread(()->send.setText(""));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}).start();

		});*/
	}
}
