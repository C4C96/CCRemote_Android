package com.ccproject.ccremote;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder>
{
	private List<Server> mServerList;
	private OnItemClickListener mOnItemClickListener;

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		TextView nameText;
		TextView ipText;

		public ViewHolder(View view)
		{
			super(view);
			nameText = (TextView) view.findViewById(R.id.ServerItem_HostName_Text);
			ipText = (TextView) view.findViewById(R.id.ServerItem_IP_Text);
		}
	}

	public ServerAdapter(List<Server> serverList)
	{
		mServerList = serverList;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.server_item, parent, false);
		ViewHolder holder = new ViewHolder(view);
		return holder;
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position)
	{
		Server server = mServerList.get(position);
		holder.nameText.setText(server.getName());
		holder.ipText.setText(server.getIp());
		if (mOnItemClickListener != null)
		{
			holder.itemView.setOnClickListener((v)->
				mOnItemClickListener.onItemClick(mServerList.get(position))
			);
		}
	}

	@Override
	public int getItemCount()
	{
		return mServerList.size();
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener)
	{
		mOnItemClickListener = onItemClickListener;
	}

	public interface OnItemClickListener
	{
		void onItemClick(Server server);
	}

}
