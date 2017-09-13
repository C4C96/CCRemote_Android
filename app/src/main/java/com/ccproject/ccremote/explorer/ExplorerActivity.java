package com.ccproject.ccremote.explorer;

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
import android.widget.Toast;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.Tools;
import com.ccproject.ccremote.baseComponent.BaseActivity;
import com.ccproject.ccremote.baseComponent.LocalServer;
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
import java.util.Stack;
import java.util.Vector;

public class ExplorerActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener, NavigationFragment.OnPathItemClickListener
{
	private List<FileSystemEntry> mFileList;
	private MultipleAdapter mAdapter;

	private NavigationFragment mNavigationFragment;

	private DrawerLayout mDrawerLayout;
	private SwipeRefreshLayout mSwipeRefresh;
	private RecyclerView mRecyclerView;

	private MenuItem mPasteItem;
	private MenuItem mBackItem;

	private Stack<String> mPathStack;
	private String mCurrentPath;

	private static final String DESKTOP = "%DESKTOP%";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explorer);

		mCurrentPath = "";
		mPathStack = new Stack<>();
		mNavigationFragment = (NavigationFragment) getSupportFragmentManager().findFragmentById(R.id.Explorer_PathFragment);
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
				mSwipeRefresh.setRefreshing(true);
				goToPath(file.getPath(), true);
			}
			else
				;// TODO
		});
		mAdapter = MultipleSelect
				.with(this)
				.adapter(adapter)
				.decorateFactory(new CheckBoxFactory(
						ContextCompat.getColor(ExplorerActivity.this, R.color.colorPrimary),
						300,
						Gravity.END | Gravity.CENTER_VERTICAL,
						10))
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
		mPasteItem = menu.findItem(R.id.explorer_toolbar_paste);
		mBackItem = menu.findItem(R.id.explorer_toolbar_back);
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
				if (!mCurrentPath.equals(""))
					myApplication.mLocalServer.send(LocalServer.PASTE_FILE, mCurrentPath.getBytes());
				break;
			case R.id.explorer_toolbar_back:
				if (!mPathStack.empty())
					goToPath(mPathStack.pop(), false);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void goToPath(String path, boolean recognize)
	{
		new Thread(()->
		{
			if (path != null && !path.equals("")) // 获取目录信息
				myApplication.mLocalServer.sendForResponse(LocalServer.GET_FILE_SYSTEM_ENTRIES,
						path.getBytes(), bytes-> goToPathCallBack(bytes, false, true, recognize));
			else // 获取磁盘信息
				myApplication.mLocalServer.sendForResponse(LocalServer.GET_DISKS,
						new byte[]{},  bytes-> goToPathCallBack(bytes, true, true, recognize));
		}).start();
	}

	private void goToPathCallBack(byte[] bytes, boolean isDisk, boolean scrollToFirst, boolean recognize)
	{
		if (recognize)
			mPathStack.add(mCurrentPath);
		mFileList.clear();
		int cursor = 0;
		if (isDisk)
		{
			mCurrentPath = "";
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
			int length = Tools.getInt(bytes, cursor);
			cursor += 4;
			try
			{
				mCurrentPath = new String(Arrays.copyOfRange(bytes, cursor, cursor + length), "UTF-8");
			} catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			cursor += length;
			mFileList.add(FileSystemEntry.getUpperDirectory(mCurrentPath));
			while (cursor + 8 <= bytes.length)
			{
				int attribute = Tools.getInt(bytes, cursor);
				length = Tools.getInt(bytes, cursor + 4);
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
			if (mBackItem != null)
				mBackItem.setVisible(!mPathStack.empty());
			mNavigationFragment.changePath(mCurrentPath);
			mSwipeRefresh.setRefreshing(false);
			if (scrollToFirst)
				mRecyclerView.scrollToPosition(0);
		});
	}

	@Override
	public void onRefresh()
	{
		goToPath(mCurrentPath, false);
	}

	@Override
	public void onNavigationItemClick(String path)
	{
		goToPath(path, true);
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

	private long backPressedTime;
	@Override
	public void onBackPressed()
	{
		if (mAdapter.getShowState() != ViewState.INSTANCE.getDEFAULT())
			mAdapter.cancel(true);
		else
		{
			if (System.currentTimeMillis() < backPressedTime + 2000)
				finish();
			else
			{
				Toast.makeText(this, R.string.DoubleClickToQuit, Toast.LENGTH_SHORT).show();
				backPressedTime = System.currentTimeMillis();
			}
		}
	}

	public static void actionStart(Context context)
	{
		Intent intent = new Intent(context, ExplorerActivity.class);
		context.startActivity(intent);
	}
}

