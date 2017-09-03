package com.ccproject.ccremote.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ccproject.ccremote.Constants;
import com.ccproject.ccremote.MyApplication;
import com.ccproject.ccremote.item.Server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;

public class ScanServerTool
{
	private static final String TAG = ScanServerTool.class.getSimpleName();

	private static final String DISCOVER_REQUEST = "nya?";
	private static final String DISCOVER_RESPONSE = "nya!";
	private static final int TIME_OUT = 500;

	/**
	 * 	扫描局域网内的服务器，异步方法
	 * */
	public static void scan(List<Server> serverList)
	{
		Log.d(TAG, "scan");
		WifiManager wifiManager = (WifiManager) MyApplication.getContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress(); //获得的ip是按ip地址的第4~1字节存储的
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
		int port = Integer.valueOf(preferences.getString("port", ""+ Constants.DEFAULT_PORT));
		serverList.clear();

		DatagramSocket socket = null;
		try
		{
			//发送UDP广播
			InetAddress broadcastIp = InetAddress.getByAddress(new byte[]{(byte) ip, (byte)(ip>>8), (byte)(ip>>16), (byte)0xFF});
			socket = new DatagramSocket();
			socket.setSoTimeout(TIME_OUT);
			DatagramPacket sendPacket = new DatagramPacket(DISCOVER_REQUEST.getBytes(), DISCOVER_REQUEST.length(), broadcastIp, port);
			socket.send(sendPacket);

			byte[] buffer = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			try
			{
				while(true)
				{
					socket.receive(receivePacket);
					String receiveMsg = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
					Log.v(TAG, "Receive UDP Message: " + receiveMsg);
					if (receiveMsg.substring(0, DISCOVER_RESPONSE.length()).equals(DISCOVER_RESPONSE))
					{
						InetAddress address = receivePacket.getAddress();
						byte[] ipBytes = address.getAddress();
						String ipStr = (ipBytes[0]&0xFF)+"."+(ipBytes[1]&0xFF)+"."+(ipBytes[2]&0xFF)+"."+(ipBytes[3]&0xFF);
						serverList.add(new Server(ipStr, receiveMsg.substring(DISCOVER_RESPONSE.length())));
					}
				}
			}
			//socket.receive方法超时跳出
			catch (SocketTimeoutException e)
			{}
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}
		finally
		{
			if (socket != null)
				socket.close();
		}

	}
}
