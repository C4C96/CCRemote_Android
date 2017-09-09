package com.ccproject.ccremote.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.Tools;
import com.ccproject.ccremote.adapter.FileAdapter;
import com.ccproject.ccremote.connection.LocalServer;
import com.ccproject.ccremote.item.Disk;
import com.ccproject.ccremote.item.FileSystemEntry;
import com.goyourfly.multiple.adapter.MultipleAdapter;
import com.goyourfly.multiple.adapter.MultipleSelect;
import com.goyourfly.multiple.adapter.StateChangeListener;
import com.goyourfly.multiple.adapter.ViewState;
import com.goyourfly.multiple.adapter.menu.CustomMenuBar;
import com.goyourfly.multiple.adapter.menu.MenuController;
import com.goyourfly.multiple.adapter.viewholder.view.CheckBoxFactory;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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

	private Menu mMenu;
	private MenuItem mPasteItem;

	private String mCurrentPath;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explorer);

		mCurrentPath = "";
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
				mCurrentPath = file.getPath();
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
				.stateChangeListener(new MultiSelectStateChangeListener())
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.explorer_toolbar, menu);
		this.mMenu = menu;
		mPasteItem = menu.findItem(R.id.explorer_toolbar_paste);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				mDrawerLayout.openDrawer(GravityCompat.START);
				break;
			case R.id.explorer_toolbar_paste:
				myApplication.mLocalServer.send(LocalServer.PASTE_FILE, mCurrentPath.getBytes());
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
			if (mCurrentPath != null && !mCurrentPath.equals("")) // 获取目录信息
				myApplication.mLocalServer.sendForResponse(LocalServer.GET_FILE_SYSTEM_ENTRIES,
											mCurrentPath.getBytes(), bytes->refreshList(bytes, false, true));
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
			mFileList.add(FileSystemEntry.getUpperDirectory(mCurrentPath));
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
			if (mPasteItem != null)
				mPasteItem.setVisible(!mCurrentPath.equals(""));
			mSwipeRefresh.setRefreshing(false);
			if (scrollToFirst)
				mRecyclerView.scrollToPosition(0);
		});
	}

	private class MultiSelectStateChangeListener implements StateChangeListener
	{
		@Override
		public void onSelectMode()
		{
			if (mCurrentPath.equals(""))
				mAdapter.cancel(false);
		}

		@Override
		public void onSelect(int i, int i1)
		{

		}

		@Override
		public void onUnSelect(int i, int i1)
		{

		}

		@Override
		public void onDone(@NotNull ArrayList<Integer> arrayList)
		{

		}

		@Override
		public void onDelete(@NotNull ArrayList<Integer> arrayList)
		{

		}

		@Override
		public void onCancel()
		{

		}
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
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			for (Integer i : select)
				Tools.writeString(bytes, mFileList.get(i).getPath());

			switch (menuItem.getItemId())
			{
				case R.id.explorer_multiselect_copy:
					myApplication.mLocalServer.send(LocalServer.COPY_FILE, bytes.toByteArray());
					mAdapter.cancel(true);
					break;
				case R.id.explorer_multiselect_cut:
					myApplication.mLocalServer.send(LocalServer.CUT_FILE, bytes.toByteArray());
					mAdapter.cancel(true);
					break;
				case R.id.explorer_multiselect_delete:
					new AlertDialog.Builder(ExplorerActivity.this)
							.setTitle(R.string.DeleteConfirmTitle)
							.setMessage(R.string.DeleteConfirmMsg)
							.setPositiveButton(R.string.Yes, (dialog, which)->
							{
								myApplication.mLocalServer.send(LocalServer.DELETE_FILE, bytes.toByteArray());
								mAdapter.cancel(true);
							})
							.setNegativeButton(R.string.No, null)
							.show();
					break;
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		if (mAdapter.getShowState() != ViewState.INSTANCE.getDEFAULT())
			mAdapter.cancel(true);
		else
			super.onBackPressed();
	}

	public static void actionStart(Context context)
	{
		Intent intent = new Intent(context, ExplorerActivity.class);
		context.startActivity(intent);
	}
}

