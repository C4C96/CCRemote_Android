package com.ccproject.ccremote;

public class Server
{
	private String mIp;
	private String mName;

	public Server(String ip, String name)
	{
		mIp = ip;
		mName = name;
	}

	public String getIp()
	{
		return mIp;
	}

	public String getName()
	{
		return mName;
	}
}
