package com.ccproject.ccremote.connection;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.widget.Toast;

import com.ccproject.ccremote.Tools;
import com.ccproject.ccremote.activity.BaseActivity;
import com.ccproject.ccremote.activity.ConnectionActivity;
import com.ccproject.ccremote.MyApplication;
import com.ccproject.ccremote.R;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class LocalServer implements SocketUtil.OnMsgReceiveListener, SocketUtil.OnDisconnectListener
{
	public static final int GET_FILE_SYSTEM_ENTRIES = 233;
	public static final int GET_DISKS = 114514;

	private SocketUtil mSocketUtil;

	private int mCount = 0;

	private SparseArray<ResponseHandle> mResponseHandles = new SparseArray<>();

	public LocalServer(String ip, int port)
	{
		mSocketUtil = new SocketUtil(ip, port);
		mSocketUtil.setOnMsgReceiveListener(this);
		mSocketUtil.setOnDisconnectListener(this);
		mSocketUtil.connect();
	}

	private final Object sendLock = new Object();
	/**
	 *  发送的格式为：长度 + 编号 + 头 + 内容
	 *               4字节  4字节 4字节
	 *  其中长度由SocketUtil类内添加
	 *  <return> 该消息的编号 </return>
	 * */
	public int send(int head,  @NonNull byte[] body)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int number;
		synchronized (sendLock)
		{
			number = mCount++;
		}
		Tools.writeInt(bytes, number);
		Tools.writeInt(bytes, head);
		bytes.write(body, 0, body.length);
		mSocketUtil.send(bytes.toByteArray());
		return number;
	}

	public void sendForResponse(int head, @NonNull byte[] body, @NonNull ResponseHandle responseHandle)
	{
		int number = send(head, body);
		mResponseHandles.append(number, responseHandle);
	}

	public void disconnect()
	{
		mSocketUtil.disconnect();
	}

	/**
	 *  收到的格式为：长度 + 编号 + 内容
	 *               4字节  4字节
	 *  其中长度已在SocketUtil类内去除
	 * */
	@Override
	public void onMsgReceive(byte[] msg)
	{
		if (msg.length < 4) return;
		int number = Tools.getInt(msg);
		ResponseHandle responseHandle = mResponseHandles.get(number);
		if (responseHandle == null) return;
		byte[] body = Arrays.copyOfRange(msg, 4, msg.length);
		responseHandle.handle(body);
		mResponseHandles.remove(number);
	}


	@Override
	public void onConnectFailed(SocketUtil socketUtil)
	{
		BaseActivity.finishExceptConnectionActivity();
		Toast.makeText(MyApplication.getContext(), Resources.getSystem().getString(R.string.Disconnected), Toast.LENGTH_SHORT).show();
	}

	public interface ResponseHandle
	{
		void handle(byte[] response);
	}
}
