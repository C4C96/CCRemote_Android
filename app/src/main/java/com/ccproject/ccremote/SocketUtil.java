package com.ccproject.ccremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

public class SocketUtil
{
	private static final String TAG = SocketUtil.class.getSimpleName();

	private String mAddress;
	private int mPort;

	private Socket mSocket;
	private OutputStream mOutStream;
	private BufferedReader mBufferedReader;

	private int mRetryDelay = 200;// 失败重连的间隔
	private int mRetryCount = 3;// 失败重连的次数，小于等于0则一直重试直到成功

	private ReceiveThread mReceiveThread;

	private OnDisconnectListener mOnDisconnectListener;
	private OnMsgReceiveListener mOnMsgReceiveListener;

	private ExecutorService mThreadPool = Executors.newFixedThreadPool(8);

	public SocketUtil(String address, int port)
	{
		mAddress = address;
		mPort = port;
	}

	private ReentrantLock lock = new ReentrantLock();
	private boolean connection_result;
	private boolean reconnect()
	{
		//只允许一个线程进行重连
		if (lock.tryLock())
		{
			close();
			int count = mRetryCount;
			while ((mSocket == null || !mSocket.isConnected() || mSocket.isClosed()) && (mRetryCount <= 0 || count-- > 0))
			{
				try
				{
					//mSocket = new Socket(mAddress, mPort);
					mSocket = new Socket();
					mSocket.connect(new InetSocketAddress(mAddress, mPort), mRetryDelay);
					InputStream inputStream = mSocket.getInputStream();
					InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
					mBufferedReader = new BufferedReader(inputStreamReader);
					mOutStream = mSocket.getOutputStream();
					mReceiveThread = new ReceiveThread();
					mReceiveThread.start();
				} catch (IOException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
			connection_result = mSocket != null && mSocket.isConnected() && !mSocket.isClosed();
			if (!connection_result)
			{
				Log.e(TAG, "Fail to connect to " + mAddress + ":" + mPort);
				close();
				if (mOnDisconnectListener != null)
					mOnDisconnectListener.onConnectFailed(this);
			}
			lock.unlock();
			return connection_result;
		} else//被阻塞的线程等结果就行
		{
			while (lock.isLocked()) ;
			return connection_result;
		}
	}

	public void connect()
	{
		mThreadPool.execute(this::reconnect);
	}

	public void send(byte[] data)
	{
		mThreadPool.execute(()->
		{
			while (true)
			{
				if (mOutStream != null)
					try
					{
						mOutStream.write(data);
						mOutStream.flush();
						break;
					} catch (IOException e)
					{
						e.printStackTrace();
						if (!reconnect())
							break;
					}
				else if (!reconnect())
					break;
			}
		});
	}

	private class ReceiveThread extends Thread
	{
		private boolean exit = false;

		public void exit()
		{
			exit = true;
		}

		@Override
		public void run()
		{
			while (!exit)
			{
				if (mBufferedReader != null)
					try
					{
						byte[] bytes = mBufferedReader.readLine().getBytes();
						if (mOnMsgReceiveListener != null)
							mThreadPool.execute(() -> mOnMsgReceiveListener.onMsgReceive(bytes));
					} catch (IOException e)
					{
						Log.e(TAG, e.getMessage());
						exit = true;//只要出错了该线程就退出，reconnect的时候会新开一个
						reconnect();
					}
				else
					break;
			}
		}
	}

	public synchronized void close()
	{
		if (mReceiveThread != null)
		{
			mReceiveThread.exit();
			mReceiveThread = null;
		}
		if (mBufferedReader != null)
		{
			try
			{
				mBufferedReader.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			mBufferedReader = null;
		}
		if (mOutStream != null)
		{
			try
			{
				mOutStream.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			mOutStream = null;
		}
		if (mSocket != null)
		{
			try
			{
				mSocket.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			mSocket = null;
		}
	}

	public interface OnDisconnectListener
	{
		void onConnectFailed(SocketUtil socketUtil);
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

	public void setOnDisconnectListener(OnDisconnectListener onDisconnectListener)
	{
		mOnDisconnectListener = onDisconnectListener;
	}

	public void setOnMsgReceiveListener(OnMsgReceiveListener onMsgReceiveListener)
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

}