package com.ccproject.ccremote.explorer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.explorer.NavigationItemAdapter;

import java.util.ArrayList;
import java.util.List;

public class NavigationFragment extends Fragment
{
	private List<String> mPathList;

    private RecyclerView mRecyclerView;
	private NavigationItemAdapter mAdapter;

	private OnPathItemClickListener mListener;

    public void changePath(String newPath)
    {
		mPathList.clear();
        String[] strings = newPath.split("\\\\");
		mPathList.add(getResources().getString(R.string.RootDirectory));
		for(String str : strings)
			if (!str.equals(""))
				mPathList.add(str);
		mAdapter.notifyDataSetChanged();
		mRecyclerView.scrollToPosition(mPathList.size() - 1);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_navigation, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.PathFragment_Recycler);
		initRecyclerView();
        return view;
    }

    private void initRecyclerView()
    {
		mPathList = new ArrayList<>();
		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
		mRecyclerView.setLayoutManager(layoutManager);
		mAdapter = new NavigationItemAdapter(mPathList);
		mRecyclerView.setAdapter(mAdapter);
		mAdapter.setOnItemClickListener(path ->
		{
			if (mListener != null)
				mListener.onNavigationItemClick(path);
		});
    }

	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		if (context instanceof OnPathItemClickListener)
		{
			mListener = (OnPathItemClickListener) context;
		} else
		{
			throw new RuntimeException(context.toString()
					+ " must implement OnPathItemClickListener");
		}
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		mListener = null;
	}

	public interface OnPathItemClickListener
	{
		void onNavigationItemClick(String path);
	}
}
