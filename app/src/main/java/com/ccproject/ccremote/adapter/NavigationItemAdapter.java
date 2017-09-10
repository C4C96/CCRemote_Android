package com.ccproject.ccremote.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ccproject.ccremote.R;

import java.util.List;

public class NavigationItemAdapter extends RecyclerView.Adapter<NavigationItemAdapter.ViewHolder>
{
	private List<String> mNavigationItemList; // 第一个是根目录，后续是除了'\'的路径部分
	private NavigationItemAdapter.OnItemClickListener mOnItemClickListener;

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		TextView text;

		public ViewHolder(View view)
		{
			super(view);
			text = (TextView) view.findViewById(R.id.navigation_item_text);
		}
	}

	public NavigationItemAdapter(List<String> pathItemListList)
	{
		mNavigationItemList = pathItemListList;
	}

	@Override
	public NavigationItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_navigation, parent, false);
		NavigationItemAdapter.ViewHolder holder = new NavigationItemAdapter.ViewHolder(view);
		return holder;
	}

	@Override
	public void onBindViewHolder(NavigationItemAdapter.ViewHolder holder, int position)
	{
		holder.text.setText(mNavigationItemList.get(position));
		if (mOnItemClickListener != null)
			holder.itemView.setOnClickListener((v)->
			{
				if (position == mNavigationItemList.size() - 1)
					return;
				if (position == 0)
					mOnItemClickListener.onItemClick("");
				else if (position == 1)
					mOnItemClickListener.onItemClick(mNavigationItemList.get(1) + "\\");
				else
				{
					StringBuilder sb = new StringBuilder();
					for (int i = 1; i < position; i++)
						sb.append(mNavigationItemList.get(i)).append("\\");
					sb.append(mNavigationItemList.get(position));
					mOnItemClickListener.onItemClick(sb.toString());
				}
			});
	}

	@Override
	public int getItemCount()
	{
		return mNavigationItemList.size();
	}

	public void setOnItemClickListener(NavigationItemAdapter.OnItemClickListener onItemClickListener)
	{
		mOnItemClickListener = onItemClickListener;
	}

	public interface OnItemClickListener
	{
		void onItemClick(String path);
	}
}
