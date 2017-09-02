package com.ccproject.ccremote;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ExplorerActivity extends AppCompatActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explorer);
	}

	class FileSystemEntry
	{
		private String path;
		private int attribute;
		private String simpleName;
		private boolean isDisk;

	}

	static class FileAttributes
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

