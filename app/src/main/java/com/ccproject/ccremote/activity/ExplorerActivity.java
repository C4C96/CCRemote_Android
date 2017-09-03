package com.ccproject.ccremote.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.ccproject.ccremote.Constants;
import com.ccproject.ccremote.R;
import com.ccproject.ccremote.Tools;
import com.ccproject.ccremote.adapter.FileAdapter;
import com.ccproject.ccremote.adapter.ServerAdapter;
import com.ccproject.ccremote.connection.LocalServer;
import com.ccproject.ccremote.item.FileSystemEntry;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ExplorerActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
	private List<FileSystemEntry> mFileList;
	private FileAdapter mAdapter;
	private SwipeRefreshLayout mSwipeRefresh;

	private String currentPath;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explorer);

		currentPath = "F:\\"; // TODO
		initToolBar();
		initRecycleView();
		initSwipeRefresh();
		mSwipeRefresh.setRefreshing(true);
		onRefresh();
	}

	private void initToolBar()
	{
		Toolbar toolbar = (Toolbar) findViewById(R.id.Explorer_Toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
		}
	}

	private void initRecycleView()
	{
		mFileList = new Vector<>();
		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.Explorer_RecyclerView);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		recyclerView.setLayoutManager(layoutManager);
		mAdapter = new FileAdapter(mFileList);
		recyclerView.setAdapter(mAdapter);
		mAdapter.setOnItemClickListener((file)->
		{
			if (file.isDirectory())
			{
				currentPath = file.getPath();
				mSwipeRefresh.setRefreshing(true);
				onRefresh();
			}
			else
				;// TODO
		});
	}

	private void initSwipeRefresh()
	{
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.Explorer_SwipeRefresh);
		mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		mSwipeRefresh.setOnRefreshListener(this);
	}
// TODO menu按钮控制滑动菜单
	@Override
	public void onRefresh()
	{
		new Thread(()->
		{
			if (currentPath != null && !currentPath.equals(""))
				myApplication.mLocalServer.sendForResponse(LocalServer.GET_FILE_SYSTEM_ENTRIES,
											currentPath.getBytes(), (bytes)->
						{
							mFileList.clear();
							int cursor = 0;
							while (cursor + 8 <= bytes.length)
							{
								int attribute = Tools.getInt(bytes, cursor);
								int length = Tools.getInt(bytes, cursor + 4);
								cursor += 8;
								if (cursor + length > bytes.length) break;
								try
								{
									String path = new String(Arrays.copyOfRange(bytes, cursor, cursor + length), "UTF-8");
									mFileList.add(new FileSystemEntry(path, attribute));
								}
								catch (UnsupportedEncodingException e)
								{}
								finally
								{
									cursor += length;
								}
							}
							runOnUiThread(()->
							{
								mAdapter.notifyDataSetChanged();
								mSwipeRefresh.setRefreshing(false);
							});
						});
			else
			{
				// TODO 获取磁盘信息
			}
		}).start();
	}

	public static void actionStart(Context context)
	{
		Intent intent = new Intent(context, ExplorerActivity.class);
		context.startActivity(intent);
	}
}

