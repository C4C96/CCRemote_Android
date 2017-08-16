package com.ccproject.ccremote;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

public class ConnectionActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
	private List<Server> mServerList;
	private ServerAdapter adapter;
	private SwipeRefreshLayout swipeRefresh;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connection);

		initRecycleView();
		initSwipeRefresh();

		//初次刷新
		swipeRefresh.setRefreshing(true);
		onRefresh();
	}

	private void initRecycleView()
	{
		mServerList = new Vector<>();
		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.Connection_RecyclerView);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		recyclerView.setLayoutManager(layoutManager);
		adapter = new ServerAdapter(mServerList);
		recyclerView.setAdapter(adapter);
		adapter.setOnItemClickListener((server)->
		{
			Log.d(TAG, "Server("+server.getIp()+") is clicked");
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConnectionActivity.this);
			int port = preferences.getInt("port", Resources.getSystem().getInteger(R.integer.default_port));
			// TODO 点击事件
		});
	}

	private void initSwipeRefresh()
	{
		swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.Connection_SwipeRefresh);
		swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		swipeRefresh.setOnRefreshListener(this);
	}

	@Override
	public void onRefresh()
	{
		new Thread(()->
		{
			ScanServerTool.scan(mServerList);
			runOnUiThread(()->
			{
				adapter.notifyDataSetChanged();
				swipeRefresh.setRefreshing(false);
			});
		}).start();
	}
}
