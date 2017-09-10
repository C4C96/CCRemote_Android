package com.ccproject.ccremote.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.item.Disk;
import com.ccproject.ccremote.item.FileSystemEntry;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder>
{
	private List<FileSystemEntry> mFileList;
	private FileAdapter.OnItemClickListener mOnItemClickListener;

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		ImageView iconImage;
		TextView nameText;

		public ViewHolder(View view)
		{
			super(view);
			iconImage = (ImageView) view.findViewById(R.id.FileItem_Icon);
			nameText = (TextView) view.findViewById(R.id.FileItem_Name_Text);
		}
	}

	public FileAdapter(List<FileSystemEntry> fileList)
	{
		mFileList = fileList;
	}

	@Override
	public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
		FileAdapter.ViewHolder holder = new FileAdapter.ViewHolder(view);
		return holder;
	}

	@Override
	public void onBindViewHolder(FileAdapter.ViewHolder holder, int position)
	{
		FileSystemEntry file = mFileList.get(position);
		holder.nameText.setText(file.getSimpleName());
		if (file instanceof Disk)
			holder.iconImage.setImageResource(R.drawable.ic_disk);
		else if (file.isDirectory())
			holder.iconImage.setImageResource(R.drawable.ic_folder);
		else
			holder.iconImage.setImageResource(R.drawable.ic_file);
		if (mOnItemClickListener != null)
		{
			holder.itemView.setOnClickListener((v)->
					mOnItemClickListener.onItemClick(mFileList.get(position))
			);
		}
	}

	@Override
	public int getItemCount()
	{
		return mFileList.size();
	}

	public void setOnItemClickListener(FileAdapter.OnItemClickListener onItemClickListener)
	{
		mOnItemClickListener = onItemClickListener;
	}

	public interface OnItemClickListener
	{
		void onItemClick(FileSystemEntry fileSystemEntry);
	}
}
