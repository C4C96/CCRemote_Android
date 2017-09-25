package com.ccproject.ccremote.explorer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.ccproject.ccremote.R;

import java.util.ArrayList;
import java.util.List;

class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder>
{
	private List<FileSystemEntry> mFileList;
	private boolean mSelectMode;

	private FileAdapter.OnItemClickListener mOnItemClickListener;
	private OnSelectModeChangeListener mOnSelectModeChangeListener;

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		CheckBox checkBox;
		ImageView iconImage;
		TextView nameText;

		public ViewHolder(View view)
		{
			super(view);
			checkBox = (CheckBox) view.findViewById(R.id.FileItem_CheckBox);
			iconImage = (ImageView) view.findViewById(R.id.FileItem_Icon);
			nameText = (TextView) view.findViewById(R.id.FileItem_Name_Text);
		}
	}

	public FileAdapter(List<FileSystemEntry> fileList)
	{
		mFileList = fileList;
		mSelectMode = false;
	}

	@Override
	public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
		return new FileAdapter.ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(FileAdapter.ViewHolder holder, int position)
	{
		final FileSystemEntry file = mFileList.get(position);
		holder.nameText.setText(file.getSimpleName());
		if (file instanceof Disk)
			holder.iconImage.setImageResource(R.drawable.ic_disk);
		else if (file.isDirectory())
			holder.iconImage.setImageResource(R.drawable.ic_folder);
		else
			holder.iconImage.setImageResource(R.drawable.ic_file);
		holder.checkBox.setVisibility(mSelectMode ? View.VISIBLE : View.GONE);
		holder.checkBox.setChecked(file.selected);
		holder.itemView.setOnClickListener((v)->
		{
			if (mSelectMode)
			{
				file.selected = !file.selected;
				notifyItemChanged(position);
			}
			else if (mOnItemClickListener != null)
				mOnItemClickListener.onItemClick(file);
		});
		holder.itemView.setOnLongClickListener((v)->
		{
			if (!mSelectMode)
			{
				changeSelectMode(true);
				file.selected = true;
				return true;
			}
			return false;
		});
	}

	public void changeSelectMode(boolean isSelect)
	{
		mSelectMode = isSelect;
		for(FileSystemEntry file : mFileList)
			file.selected = false;
		notifyDataSetChanged();
		if (mOnSelectModeChangeListener != null)
			mOnSelectModeChangeListener.onSelectModeChange(isSelect);
	}

	public boolean isSelectMode()
	{
		return mSelectMode;
	}

	public List<FileSystemEntry> getSelected()
	{
		List<FileSystemEntry> ret = new ArrayList<>();
		for (FileSystemEntry file : mFileList)
			if (file.selected)
				ret.add(file);
		return ret;
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

	public void setOnSelectModeChangeListener(OnSelectModeChangeListener onSelectModeChangeListener)
	{
		mOnSelectModeChangeListener = onSelectModeChangeListener;
	}

	public interface OnItemClickListener
	{
		void onItemClick(FileSystemEntry fileSystemEntry);
	}

	public interface OnSelectModeChangeListener
	{
		void onSelectModeChange(boolean isSelectMode);
	}
}
