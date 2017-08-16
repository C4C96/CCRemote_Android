package com.ccproject.ccremote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketUtil
{
	private static final String TAG = SocketUtil.class.getSimpleName();

	private String mAddress;
	private int mPort;
	private Socket mSocket;

	private int mRetryDelay = 500;// 失败重连的间隔
	private int mRetryCount = 5;// 失败重连的次数，小于等于0则一直重试直到成功
	private boolean mHasFailed = false;

	private int mBufferSize = 4096;
	private Thread mReceiveThread;

	private OnConnectFailedListener mOnConnectFailedListener;
	private OnMsgReceiveListener mOnMsgReceiveListener;

	public SocketUtil(String address, int port)
	{
		mAddress = address;
		mPort = port;

		mReceiveThread = new Thread(new Receive());
		mReceiveThread.start();
	}

	private boolean connect()
	{
		if (mHasFailed) return false;

		synchronized (mSocket)
		{
			int count = mRetryCount;
			while ((mSocket == null || mSocket.isClosed()) && (mRetryCount <= 0 || count-- > 0))
			{
				try
				{
					mSocket = new Socket(mAddress, mPort);
				} catch (IOException e)
				{
					Log.e(TAG, e.getMessage());
					try
					{
						Thread.sleep(mRetryDelay);
					} catch (InterruptedException e1)
					{
						e1.printStackTrace();
					}
				}
			}
			if (mSocket != null && !mSocket.isClosed())
				return true;
			else
			{
				Log.e(TAG, "Fail to connect to " + mAddress + ":" + mPort);
				mHasFailed = true;
				if (mOnConnectFailedListener != null)
					mOnConnectFailedListener.onConnectFailed();
				return false;
			}
		}
	}

	public void send(byte[] data)
	{
		new Thread(()->
		{
			if (!connect())	return;
			OutputStream out = null;
			try
			{
				out = mSocket.getOutputStream();
				out.write(data);
				out.flush();
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage());
			}
			finally
			{
				if (out != null)
					try
					{
						out.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
			}
		}).start();
	}

	private class Receive implements Runnable
	{
		@Override
		public void run()
		{
			InputStream in = null;
			int length;
			byte[] buffer = new byte[mBufferSize];
			while (connect())
			{
				try
				{
					in = mSocket.getInputStream();
					ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
					while((length = in.read(buffer)) != -1)
					{
						byteArray.write(buffer, 0, length);
					}
					if (mOnMsgReceiveListener != null)
						mOnMsgReceiveListener.onMsgReceive(byteArray.toByteArray());
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage());
				}
				finally
				{
					if (in != null)
						try
						{
							in.close();
						} catch (IOException e)
						{
							e.printStackTrace();
						}
				}
			}
		}
	}

	public void close()
	{
		mHasFailed = true;
		if (mSocket != null)
		{
			if (!mSocket.isClosed())
				try
				{
					mSocket.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			mSocket = null;
		}
	}

	public interface OnConnectFailedListener
	{
		void onConnectFailed();
	}

	public interface OnMsgReceiveListener
	{
		void onMsgReceive(byte[] msg);
	}

	public void setRetryDelay(int retryDelay)
	{
		mRetryDelay = retryDelay;
	}

	public void setRetryCount(int retryCount)
	{
		mRetryCount = retryCount;
	}

	public void setBufferSize(int bufferSize)
	{
		mBufferSize = bufferSize;
	}

	public void setOnConnectFailedListener(OnConnectFailedListener onConnectFailedListener)
	{
		mOnConnectFailedListener = onConnectFailedListener;
	}

	public void setmOnMsgReceiveListener(OnMsgReceiveListener onMsgReceiveListener)
	{
		mOnMsgReceiveListener = onMsgReceiveListener;
	}

	public String getAddress()
	{
		return mAddress;
	}

	public int getPort()
	{
		return mPort;
	}

	public int getRetryDelay()
	{
		return mRetryDelay;
	}

	public int getRetryCount()
	{
		return mRetryCount;
	}

	public boolean getHasFailed()
	{
		return mHasFailed;
	}

	public int getBufferSize()
	{
		return mBufferSize;
	}
}