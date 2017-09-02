package com.ccproject.ccremote;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

public class SocketUtil
{
	private static final String TAG = SocketUtil.class.getSimpleName();

	private static final int BUFFER_SIZE = 1024;

	private String mAddress;
	private int mPort;

	private Socket mSocket;
	private OutputStream mOut;
	private InputStream mIn;

	private int mRetryDelay = 200;// 失败重连的间隔
	private int mRetryCount = 3;// 失败重连的次数，小于等于0则一直重试直到成功

	private boolean needToConnect = false;

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
		if (!needToConnect) return false;
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
		needToConnect = true;
		mThreadPool.execute(this::reconnect);
	}

	public void send(byte[] data)
	{
		mThreadPool.execute(()->
		{
			while (true)
			{
				if (mOut != null)
					try
					{
						int size = data.length + 4;
						ByteArrayOutputStream bytes = new ByteArrayOutputStream();
						bytes.write(size >> 24);
						bytes.write(size >> 16);
						bytes.write(size >> 8);
						bytes.write(size);
						bytes.write(data, 0, data.length);
						mOut.write(/*data*/bytes.toByteArray());
						mOut.flush();
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
			byte[] buffer = new byte[BUFFER_SIZE];
			while (!exit)
			{
				if (mIn != null)
					try
					{
						int count = mIn.read(buffer);
						if (count < 4) continue;
						// 前4个字节表示消息长度
						int length = (buffer[0] << 24)
									+ (buffer[1] << 16)
									+ (buffer[2] << 8)
									+ buffer[3];
						ByteArrayOutputStream bytes = new ByteArrayOutputStream();
						bytes.write(buffer, 4, buffer.length - 4);
						int remainLength = length - count;
						while (remainLength > 0)
						{
							count = mIn.read(buffer);
							bytes.write(buffer, 0, count);
							remainLength -= count;
						}

						if (mOnMsgReceiveListener != null)
							mThreadPool.execute(() -> mOnMsgReceiveListener.onMsgReceive(bytes.toByteArray()));
					} catch (Exception e)
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

	private synchronized void close()
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
				mSocket.shutdownInput();
				mSocket.shutdownOutput();
				mSocket.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			mSocket = null;
		}
	}

	public void disconnect()
	{
		needToConnect = false;
		mThreadPool.execute(this::close);
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