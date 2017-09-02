package com.ccproject.ccremote;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;


public class LocalServer implements SocketUtil.OnMsgReceiveListener
{
	private SocketUtil mSocketUtil;

	private int mCount = 0;

	private SparseArray<ResponseHandle> mResponseHandles = new SparseArray<>();

	public LocalServer(String ip, int port)
	{
		mSocketUtil = new SocketUtil(ip, port);
		mSocketUtil.setOnMsgReceiveListener(this);
		mSocketUtil.connect();
	}

	public void setOnDisconnectListener(SocketUtil.OnDisconnectListener onDisconnectListener)
	{
		mSocketUtil.setOnDisconnectListener(onDisconnectListener);
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
		bytes.write(number >> 24);
		bytes.write(number >> 16);
		bytes.write(number >> 8);
		bytes.write(number);
		bytes.write(head >> 24);
		bytes.write(head >> 16);
		bytes.write(head >> 8);
		bytes.write(head);
		bytes.write(body, 0, body.length);
		mSocketUtil.send(bytes.toByteArray());
		return number;
	}

	public void sendForResponse(int head, @NonNull byte[] body, @NonNull ResponseHandle responseHandle)
	{
		int count = send(head, body);
		mResponseHandles.append(count, responseHandle);
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
		int number = (msg[0] << 24)
					+ (msg[1] << 16)
					+ (msg[2] << 8)
					+ msg[3];
		ResponseHandle responseHandle = mResponseHandles.get(number);
		if (responseHandle == null) return;
		byte[] body = Arrays.copyOfRange(msg, 4, msg.length);
		responseHandle.handle(body);
		mResponseHandles.remove(number);
	}

	public interface ResponseHandle
	{
		void handle(byte[] response);
	}
}
