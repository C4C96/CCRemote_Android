package com.ccproject.ccremote;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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

		Toolbar toolbar = (Toolbar) findViewById(R.id.Connection_Toolbar);
		setSupportActionBar(toolbar);

		initRecycleView();
		initSwipeRefresh();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		swipeRefresh.setRefreshing(true);
		onRefresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.connection_toolbar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.connection_options_settings:
				Intent intent = new Intent(this, MyPreferenceActivity.class);
				startActivity(intent);
				break;
			case R.id.connection_options_user_manual:
				//TODO
				break;
			case R.id.connection_options_about:
				//TODO
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
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
			int port = Integer.valueOf(preferences.getString("port", ""+Constants.DEFAULT_PORT));
			MainActivity.actionStart(ConnectionActivity.this, server.getIp(), port);
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
