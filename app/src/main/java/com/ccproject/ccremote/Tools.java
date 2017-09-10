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

	public static String getString(byte[] bytes, int index)
	{
		if (index + 4 > bytes.length)
			return null;
		int strLength = Tools.getInt(bytes, index);
		index += 4;
		if (index + strLength > bytes.length)
			return null;
		String ret = null;
		try
		{
			ret = new String(Arrays.copyOfRange(bytes, index, index + strLength), "UTF-8");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return ret;
	}

	public static String getString(byte[] bytes)
	{
		return getString(bytes, 0);
	}

	public static String[] getStrings(byte[] bytes, int index)
	{
		String str;
		int cursor = index;
		List<String> ret = new ArrayList<>();
		while ((str = getString(bytes, cursor)) != null)
		{
			ret.add(str);
			cursor += str.getBytes().length + 4;
		}
		return ret.toArray(new String[ret.size()]);
	}

	public static void writeString(ByteArrayOutputStream bytes, String string)
	{
		byte[] strBytes = string.getBytes();
		writeInt(bytes, strBytes.length);
		bytes.write(strBytes, 0, strBytes.length);
	}

}
