package com.ccproject.ccremote.explorer;

public class FileSystemEntry
{
	protected String mPath;
	protected int mAttribute;
	protected String mSimpleName;
	protected String mExtension;

	protected FileSystemEntry(){}

	public boolean selected = false;

	public FileSystemEntry(String path, int attribute)
	{
		this.mPath = path;
		this.mAttribute = attribute;
		this.mSimpleName = path.substring(path.lastIndexOf("\\") + 1);
		int tmp = mSimpleName.lastIndexOf(".");
		mExtension = tmp >= 0 ? mSimpleName.substring(tmp+1) : "";
	}

	public boolean isDirectory()
	{
		return (mAttribute & FileAttributes.Directory) == FileAttributes.Directory;
	}

	public String getPath()
	{
		return mPath;
	}

	public int getAttribute()
	{
		return mAttribute;
	}

	public String getSimpleName()
	{
		return mSimpleName;
	}

	public String getExtension()
	{
		return mExtension;
	}

	public static class FileAttributes
	{
		public static int ReadOnly = 1;
		public static int Hidden = 2;
		public static int System = 4;
		public static int Directory = 16;
		public static int Archive = 32;
		public static int Device = 64;
		public static int Normal = 128;
		public static int Temporary = 256;
		public static int SparseFile = 512;
		public static int ReparsePoint = 1024;
		public static int Compressed = 2048;
		public static int Offline = 4096;
		public static int NotContentIndexed = 8192;
		public static int Encrypted = 16384;
		public static int IntegrityStream = 32768;
		public static int NoScrubData = 131072;
	}
}
