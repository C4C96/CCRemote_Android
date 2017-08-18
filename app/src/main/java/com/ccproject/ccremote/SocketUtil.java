package com.ccproject.ccremote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

public class SocketUtil
{
	private static final String TAG = SocketUtil.class.getSimpleName();

	private String mAddress;
	private int mPort;

	private Socket mSocket;
	private InputStream mIn;
	private OutputStream mOut;

	private int mRetryDelay = 200;// 失败重连的间隔
	private int mRetryCount = 3;// 失败重连的次数，小于等于0则一直重试直到成功

	private int mBufferSize = 1024;
	private ReceiveThread mReceiveThread;

	private OnConnectFailedListener mOnConnectFailedListener;
	private OnMsgReceiveListener mOnMsgReceiveListener;

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
					mIn = mSocket.getInputStream();
					mOut = mSocket.getOutputStream();
					mReceiveThread = new ReceiveThread();
					mReceiveThread.start();
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
			connection_result = mSocket != null && mSocket.isConnected() && !mSocket.isClosed();
			if (!connection_result)
			{
				Log.e(TAG, "Fail to connect to " + mAddress + ":" + mPort);
				close();
				if (mOnConnectFailedListener != null)
					mOnConnectFailedListener.onConnectFailed(this);
			}
			lock.unlock();
			return connection_result;
		}
		else//被阻塞的线程等结果就行
		{
			while(lock.isLocked());
			return connection_result;
		}
	}

	public void connect()
	{
		new Thread(this::reconnect).start();
	}

	public void send(byte[] data)
	{
		//TODO 改用线程池
		new Thread(()->
		{
			while (true)
			{
				if (mOut != null)
					try
					{
						mOut.write(data);
						mOut.flush();
						break;
					}
					catch (IOException e)
					{
						e.printStackTrace();
						if (!reconnect())
							break;
					}
				else if (!reconnect())
					break;
			}
		}).start();
	}

	private class ReceiveThread extends Thread
	{
		private boolean exit = false;

		public void exit(){exit = true;}

		@Override
		public void run()
		{
			int length;
			byte[] buffer = new byte[mBufferSize];
			while (!exit)
			{
				if (mIn != null)
					try
					{
						ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
						while((length = mIn.read(buffer)) != -1)
						{
							byteArray.write(buffer, 0, length);
						}
						if (mOnMsgReceiveListener != null)
							new Thread(()->mOnMsgReceiveListener.onMsgReceive(byteArray.toByteArray())).start();//TODO 用线程池
					}
					catch (IOException e)
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
		if (mIn != null)
		{
			try
			{
				mIn.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			mIn = null;
		}
		if (mOut != null)
		{
			try
			{
				mOut.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			mOut = null;
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

	public interface OnConnectFailedListener
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

	public void setBufferSize(int bufferSize)
	{
		mBufferSize = bufferSize;
	}

	public void setOnConnectFailedListener(OnConnectFailedListener onConnectFailedListener)
	{
		mOnConnectFailedListener = onConnectFailedListener;
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

	public int getBufferSize()
	{
		return mBufferSize;
	}
}