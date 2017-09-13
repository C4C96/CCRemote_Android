package com.ccproject.ccremote.explorer;

public class Disk extends FileSystemEntry
{
	public Disk(String path, String label)
	{
		mPath = path;
		mAttribute = FileAttributes.System | FileAttributes.Hidden | FileAttributes.Directory;
		mSimpleName = label + " (" +  path.toUpperCase().charAt(0) + ":)";
		mExtension = "";
	}
}
