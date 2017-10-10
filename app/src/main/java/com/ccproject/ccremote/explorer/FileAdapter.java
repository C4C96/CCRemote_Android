package com.ccproject.ccremote.explorer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ccproject.ccremote.MyApplication;
import com.ccproject.ccremote.R;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.implments.SwipeItemMangerImpl;
import com.daimajia.swipe.implments.SwipeItemRecyclerMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import java.util.ArrayList;
import java.util.List;

class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> implements SwipeItemMangerInterface, SwipeAdapterInterface
{
	private List<FileSystemEntry> mFileList;
	private boolean mSelectMode;

	private SwipeItemMangerImpl mItemManager = new SwipeItemRecyclerMangerImpl(this);

	private FileAdapter.OnItemClickListener mOnItemClickListener;
	private OnSelectModeChangeListener mOnSelectModeChangeListener;
	private OnButtonClickListener mOnButtonClickListener;

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		CheckBox checkBox;
		ImageView iconImage;
		TextView nameText;

		boolean canBeClick = true;

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
			if (!holder.canBeClick)
				return;
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
			if (!holder.canBeClick)
				return false;
			if (file instanceof Disk)
				return false;
			if (!mSelectMode)
			{
				changeSelectMode(true);
				file.selected = true;
				return true;
			}
			return false;
		});
		holder.canBeClick = true;

		SwipeLayout swipe = (SwipeLayout)holder.itemView.findViewById(R.id.FileItem_Swipe);
		mItemManager.bindView(swipe, position);
		swipe.addSwipeListener(new SwipeLayout.SwipeListener()
		{
			@Override
			public synchronized void onStartOpen(SwipeLayout layout)
			{
				if (mSelectMode)
				{
					layout.close(false);
					return;
				}
				holder.canBeClick = false;
			}

			@Override
			public void onOpen(SwipeLayout layout)
			{

			}

			@Override
			public void onStartClose(SwipeLayout layout)
			{

			}

			@Override
			public void onClose(SwipeLayout layout)
			{
				holder.canBeClick = true;
			}

			@Override
			public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset)
			{

			}

			@Override
			public void onHandRelease(SwipeLayout layout, float xvel, float yvel)
			{

			}
		});

		if (mOnButtonClickListener != null)
		{
			holder.itemView.findViewById(R.id.FileItem_Open).setOnClickListener((v ->
			{
				mOnButtonClickListener.onButtonClick(R.id.FileItem_Open, file);
				swipe.close(true);
			}));
			holder.itemView.findViewById(R.id.FileItem_Property).setOnClickListener((v)->
			{
				mOnButtonClickListener.onButtonClick(R.id.FileItem_Property, file);
				swipe.close(true);
			});
		}
	}

	public void changeSelectMode(boolean isSelect)
	{
		mSelectMode = isSelect;
		mItemManager.closeAllItems();
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

	public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener)
	{
		mOnButtonClickListener = onButtonClickListener;
	}

	public interface OnItemClickListener
	{
		void onItemClick(FileSystemEntry fileSystemEntry);
	}

	public interface OnSelectModeChangeListener
	{
		void onSelectModeChange(boolean isSelectMode);
	}

	public interface OnButtonClickListener
	{
		void onButtonClick(int id, FileSystemEntry fileSystemEntry);
	}

	@Override
	public int getSwipeLayoutResourceId(int position)
	{
		return R.id.FileItem_Swipe;
	}



	//以下是SwipeItemMangerInterface的实现，因为SwipeItemMangerImpl里的bug，所以不得不写这些废话
	@Override
	public void openItem(int position)
	{

	}

	@Override
	public void closeItem(int position)
	{

	}

	@Override
	public void closeAllExcept(SwipeLayout layout)
	{

	}

	@Override
	public void closeAllItems()
	{

	}

	@Override
	public List<Integer> getOpenItems()
	{
		return null;
	}

	@Override
	public List<SwipeLayout> getOpenLayouts()
	{
		return null;
	}

	@Override
	public void removeShownLayouts(SwipeLayout layout)
	{

	}

	@Override
	public boolean isOpen(int position)
	{
		return false;
	}

	@Override
	public Attributes.Mode getMode()
	{
		return null;
	}

	@Override
	public void setMode(Attributes.Mode mode)
	{

	}
}
