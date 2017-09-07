package com.ccproject.ccremote.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.Tools;
import com.ccproject.ccremote.adapter.FileAdapter;
import com.ccproject.ccremote.connection.LocalServer;
import com.ccproject.ccremote.item.Disk;
import com.ccproject.ccremote.item.FileSystemEntry;
import com.goyourfly.multiple.adapter.MultipleAdapter;
import com.goyourfly.multiple.adapter.MultipleSelect;
import com.goyourfly.multiple.adapter.menu.CustomMenuBar;
import com.goyourfly.multiple.adapter.menu.MenuController;
import com.goyourfly.multiple.adapter.menu.SimpleDeleteSelectAllMenuBar;
import com.goyourfly.multiple.adapter.viewholder.view.CheckBoxFactory;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ExplorerActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener
{
	private List<FileSystemEntry> mFileList;
	//private FileAdapter mAdapter;
	private MultipleAdapter mAdapter;

	private DrawerLayout mDrawerLayout;
	private SwipeRefreshLayout mSwipeRefresh;
	private RecyclerView mRecyclerView;

	private String currentPath;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explorer);

		currentPath = "";
		mDrawerLayout = (DrawerLayout) findViewById(R.id.Explorer_Drawer);
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
		mRecyclerView = (RecyclerView) findViewById(R.id.Explorer_RecyclerView);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(layoutManager);
		FileAdapter adapter = new FileAdapter(mFileList);
		adapter.setOnItemClickListener((file)->
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
		mAdapter = MultipleSelect
				.with(this)
				.adapter(adapter)
				.decorateFactory(new CheckBoxFactory())
				.customMenu(new MultiSelectMenuBar(this,
						R.menu.explorer_multiselect,
						ContextCompat.getColor(ExplorerActivity.this, R.color.colorPrimary),
						Gravity.TOP))
				.build();
		mRecyclerView.setAdapter(mAdapter);
		mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
	}

	private void initSwipeRefresh()
	{
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.Explorer_SwipeRefresh);
		mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		mSwipeRefresh.setOnRefreshListener(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				mDrawerLayout.openDrawer(GravityCompat.START);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onRefresh()
	{
		new Thread(()->
		{
			if (currentPath != null && !currentPath.equals("")) // 获取目录信息
				myApplication.mLocalServer.sendForResponse(LocalServer.GET_FILE_SYSTEM_ENTRIES,
											currentPath.getBytes(), bytes->refreshList(bytes, false, true));
			else // 获取磁盘信息
				myApplication.mLocalServer.sendForResponse(LocalServer.GET_DISKS,
											new byte[]{},  bytes->refreshList(bytes, true, true));
		}).start();
	}

	private void refreshList(byte[] bytes, boolean isDisk, boolean scrollToFirst)
	{
		mFileList.clear();
		int cursor = 0;
		if (isDisk)
		{
			while (cursor + 7 <= bytes.length)
			{
				try
				{
					String path = new String(Arrays.copyOfRange(bytes, cursor, cursor + 3), "UTF-8");
					int labelLength = Tools.getInt(bytes, cursor + 3);
					cursor += 7;
					if (cursor + labelLength > bytes.length) break;
					String label = new String(Arrays.copyOfRange(bytes, cursor, cursor + labelLength), "UTF-8");
					mFileList.add(new Disk(path, label));
					cursor += labelLength;
				} catch (UnsupportedEncodingException e)
				{
					break;
				}
			}
		}
		else
		{
			mFileList.add(FileSystemEntry.getUpperDirectory(currentPath));
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
		}
		runOnUiThread(()->
		{
			mAdapter.cancel(false);
			mAdapter.notifyDataSetChanged();
			mSwipeRefresh.setRefreshing(false);
			if (scrollToFirst)
				mRecyclerView.scrollToPosition(0);
		});
	}

	private class MultiSelectMenuBar extends CustomMenuBar
	{
		public MultiSelectMenuBar(@NotNull Activity activity, int menuId, int menuBgColor, int gravity)
		{
			super(activity, menuId, menuBgColor, gravity);
		}

		@Override
		public void onMenuItemClick(@NotNull MenuItem menuItem, @NotNull MenuController menuController)
		{
			List<Integer> select = mAdapter.getSelect();
			switch (menuItem.getItemId()) // TODO
			{
				case R.id.explorer_multiselect_copy:
					Toast.makeText(ExplorerActivity.this, select.size()+" items will be copied.", Toast.LENGTH_SHORT).show();
					break;
				case R.id.explorer_multiselect_cut:
					Toast.makeText(ExplorerActivity.this, select.size()+" items will be cut.", Toast.LENGTH_SHORT).show();
					break;
				case R.id.explorer_multiselect_delete:
					Toast.makeText(ExplorerActivity.this, select.size()+" items will be deleted.", Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}

	public static void actionStart(Context context)
	{
		Intent intent = new Intent(context, ExplorerActivity.class);
		context.startActivity(intent);
	}
}

