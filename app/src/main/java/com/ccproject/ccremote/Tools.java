package com.ccproject.ccremote;

import java.io.ByteArrayOutputStream;

public class Tools
{
	/**
	 * 把四个字节拼接成int
	 */
	public static int getInt(byte[] bytes, int index)
	{
		return (bytes[index] << 24)
				| (bytes[index + 1] << 16)
				| (bytes[index + 2] << 8)
				| (bytes[index + 3] & 0xFF);
	}

	public static int getInt(byte[] bytes)
	{
		return getInt(bytes, 0);
	}

	/**
	 * 将int作为四个字节写入ByteArrayOutputStream
	 * */
	public static void writeInt(ByteArrayOutputStream bytes, int i)
	{
		bytes.write(i >>> 24);
		bytes.write(i >>> 16);
		bytes.write(i >>> 8);
		bytes.write(i & 0xFF);
	}

	public static void writeString(ByteArrayOutputStream bytes, String string)
	{
		byte[] strBytes = string.getBytes();
		writeInt(bytes, strBytes.length);
		bytes.write(strBytes, 0, strBytes.length);
	}

}
