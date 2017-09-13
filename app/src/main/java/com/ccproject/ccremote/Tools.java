package com.ccproject.ccremote;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	public static String getString(byte[] bytes, int index, int length)
	{
		if (index + length > bytes.length)
			return null;
		String ret = null;
		try
		{
			ret = new String(Arrays.copyOfRange(bytes, index, index + length), "UTF-8");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return ret;
	}

	public static String getString(byte[] bytes, int length)
	{
		return getString(bytes, 0, length);
	}

	public static void writeString(ByteArrayOutputStream bytes, String string)
	{
		byte[] strBytes = string.getBytes();
		writeInt(bytes, strBytes.length);
		bytes.write(strBytes, 0, strBytes.length);
	}

	public static long getLong(byte[] bytes, int index)
	{
		  return ((((long) bytes[index] & 0xff) << 56)
		       | (((long) bytes[index + 1] & 0xff) << 48)
		       | (((long) bytes[index + 2] & 0xff) << 40)
		       | (((long) bytes[index + 3] & 0xff) << 32)
		       | (((long) bytes[index + 4] & 0xff) << 24)
		       | (((long) bytes[index + 5] & 0xff) << 16)
		       | (((long) bytes[index + 6] & 0xff) << 8)
			   | (((long) bytes[index + 7] & 0xff)));
	}

	public static long getLong(byte[] bytes)
	{
		return getLong(bytes, 0);
	}

}
