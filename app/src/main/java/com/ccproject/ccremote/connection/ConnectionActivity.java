package com.ccproject.ccremote.connection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.MainActivity;
import com.ccproject.ccremote.baseComponent.BaseActivity;
import com.ccproject.ccremote.preference.MyPreferenceActivity;

import java.util.List;
import java.util.Vector;

public class ConnectionActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
	private List<Server> mServerList;
	private ServerAdapter mAdapter;
	private SwipeRefreshLayout mSwipeRefresh;
	private RecyclerView mRecyclerView;

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
		mSwipeRefresh.setRefreshing(true);
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
				//TODO 显示使用说明，可弹网页
				break;
			case R.id.connection_options_about:
				//TODO 显示关于，版权，balabala……
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void initRecycleView()
	{
		mServerList = new Vector<>();
		mRecyclerView = (RecyclerView) findViewById(R.id.Connection_RecyclerView);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(layoutManager);
		mAdapter = new ServerAdapter(mServerList);
		mRecyclerView.setAdapter(mAdapter);
		mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
		mAdapter.setOnItemClickListener((server)->
		{
			Log.d(TAG, "Server("+server.getIp()+") is clicked");
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConnectionActivity.this);
			int port = Integer.valueOf(preferences.getString("port", ""+ getResources().getInteger(R.integer.default_port)));
			MainActivity.actionStart(ConnectionActivity.this, server.getIp(), port);
		});
	}

	private void initSwipeRefresh()
	{
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.Connection_SwipeRefresh);
		mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		mSwipeRefresh.setOnRefreshListener(this);
	}

	@Override
	public void onRefresh()
	{
		new Thread(()->
		{
			ScanServerTool.scan(mServerList);
			runOnUiThread(()->
			{
				mAdapter.notifyDataSetChanged();
				mSwipeRefresh.setRefreshing(false);
				mRecyclerView.scrollToPosition(0);
			});
		}).start();
	}
}
