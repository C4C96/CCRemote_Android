package com.ccproject.ccremote.item;

public class Disk extends FileSystemEntry
{
	public Disk(String path, int attribute, String label)
	{
		mPath = path;
		mAttribute = attribute;
		mSimpleName = label + " (" +  path.toUpperCase().charAt(0) + ":)";
		mExtension = "";
	}
}
