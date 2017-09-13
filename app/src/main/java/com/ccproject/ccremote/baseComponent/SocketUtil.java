package com.ccproject.ccremote.baseComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.ccproject.ccremote.Tools;

public class SocketUtil
{
	private static final String TAG = SocketUtil.class.getSimpleName();

	private static final int BUFFER_SIZE = 1024;
	private static final int HEART_BEAT_DELAY = 2000;

	private String mAddress;
	private int mPort;

	private Socket mSocket;
	private OutputStream mOut;
	private InputStream mIn;

	private int mRetryDelay = 200; // 失败重连的间隔
	private int mRetryCount = 3; // 失败重连的次数，小于等于0则一直重试直到成功

	private boolean needToConnect = false;

	private ReceiveThread mReceiveThread;
	private HeartBeatThread mHeartBeatThread;

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
					mSocket = new Socket();
					mSocket.connect(new InetSocketAddress(mAddress, mPort), mRetryDelay);
					mIn = mSocket.getInputStream();
					mOut = mSocket.getOutputStream();
					mReceiveThread = new ReceiveThread();
					mReceiveThread.start();
					mHeartBeatThread = new HeartBeatThread();
					mHeartBeatThread.start();
					receiveTime = System.currentTimeMillis();
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
						Tools.writeInt(bytes, size);
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
						receiveTime = System.currentTimeMillis();
						if (count < 4) continue;
						// 前4个字节表示消息长度
						int length = Tools.getInt(buffer);
						ByteArrayOutputStream bytes = new ByteArrayOutputStream();
						bytes.write(buffer, 4, count - 4);
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

	private long receiveTime;
	private class HeartBeatThread extends Thread
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
				long time = System.currentTimeMillis();
				if (time >= receiveTime + HEART_BEAT_DELAY) // 若在一定时间内无消息，则断连
				{
					exit = true;
					reconnect();
				}
				else
					try
					{
						Thread.sleep(HEART_BEAT_DELAY - (time - receiveTime));
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}
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
		if (mHeartBeatThread != null)
		{
			mHeartBeatThread.exit();
			mHeartBeatThread = null;
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