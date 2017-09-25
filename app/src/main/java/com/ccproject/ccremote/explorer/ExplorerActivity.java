package com.ccproject.ccremote.explorer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.ccproject.ccremote.R;
import com.ccproject.ccremote.Tools;
import com.ccproject.ccremote.baseComponent.BaseActivity;
import com.ccproject.ccremote.baseComponent.LocalServer;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

public class ExplorerActivity extends BaseActivity implements NavigationFragment.OnPathItemClickListener
{
	private List<FileSystemEntry> mFileList;
	private FileAdapter mAdapter;

	private NavigationFragment mNavigationFragment;

	private DrawerLayout mDrawerLayout;
	private SwipeRefreshLayout mSwipeRefresh;
	private RecyclerView mRecyclerView;

	private MenuItem mUpperItem;
	private MenuItem mBackItem;
	private MenuItem mForwardItem;

	private FloatingActionMenu mFloatingMenu;

	private Stack<String> mBackStack;
	private Stack<String> mForwardStack;

	private String mCurrentPath;

	private static final String DESKTOP = "%DESKTOP%";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_explorer);

		mCurrentPath = "";
		mBackStack = new Stack<>();
		mForwardStack = new Stack<>();
		mNavigationFragment = (NavigationFragment) getSupportFragmentManager().findFragmentById(R.id.Explorer_PathFragment);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.Explorer_Drawer);
		initToolBar();
		initRecycleView();
		initSwipeRefresh();
		initFloatingMenu();
		mSwipeRefresh.setRefreshing(true);
		goToPath(mCurrentPath, false);
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
		mAdapter = new FileAdapter(mFileList);
		mAdapter.setOnItemClickListener((file)->
		{
			if (file.isDirectory())
			{
				mSwipeRefresh.setRefreshing(true);
				goToPath(file.getPath(), true);
			}
			else
				;// TODO
		});
		mAdapter.setOnSelectModeChangeListener(isSelectMode ->
		{
			if (isSelectMode && mCurrentPath.equals(""))
			{
				mAdapter.changeSelectMode(false);
				return;
			}
			// TODO 变toolbar

		});
		mRecyclerView.setAdapter(mAdapter);
		mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
	}

	private void initSwipeRefresh()
	{
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.Explorer_SwipeRefresh);
		mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		mSwipeRefresh.setOnRefreshListener(()->goToPath(mCurrentPath, false));
	}

	private void initFloatingMenu()
	{
		mFloatingMenu = (FloatingActionMenu) findViewById(R.id.explorer_floatingMenu);
		mFloatingMenu.setOnClickListener((v)->
		{
			if (mFloatingMenu.isOpened())
				mFloatingMenu.close(true);
		});
		mFloatingMenu.setClickable(false);
		mFloatingMenu.setOnMenuToggleListener(mFloatingMenu::setClickable);

		if (mCurrentPath.equals(""))
			mFloatingMenu.hideMenu(false);
		else
			mFloatingMenu.showMenu(false);

		FloatingActionButton paste = (FloatingActionButton) findViewById(R.id.explorer_paste_button);
		paste.setOnClickListener((v)->
		{
			if (!mCurrentPath.equals(""))
				myApplication.mLocalServer.send(LocalServer.PASTE_FILE, mCurrentPath.getBytes());
			mFloatingMenu.close(true);
		});

		FloatingActionButton newFolder = (FloatingActionButton) findViewById(R.id.explorer_new_folder_button);
		newFolder.setOnClickListener((v)->
		{
			final char[] illegalChar = new char[]{'\\', '/', ':', '*', '?', '\"', '<', '>', '|'};
			final EditText editText = new EditText(ExplorerActivity.this);
			editText.setMaxLines(255);
			new AlertDialog.Builder(ExplorerActivity.this)
					.setTitle(R.string.InputNewFolderName)
					.setView(editText)
					.setPositiveButton(R.string.Confirm, (dialog, which)->
					{
						String folderName = editText.getText().toString();
						for (char c : illegalChar)
							if (folderName.indexOf(c) != -1)
							{
								Toast.makeText(ExplorerActivity.this, R.string.FolderNameError, Toast.LENGTH_SHORT).show();
								return;
							}
						String path = mCurrentPath.charAt(mCurrentPath.length() - 1) == '\\' ?
										mCurrentPath + folderName :
										mCurrentPath + "\\" + folderName;
						myApplication.mLocalServer.send(LocalServer.CREATE_DIRECTORY, path.getBytes());
					})
					.setNegativeButton(R.string.Cancel, null)
					.show();
		});

		FloatingActionButton property = (FloatingActionButton) findViewById(R.id.explorer_property_button);
		property.setOnClickListener((v)->
		{
			//TODO
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.explorer_toolbar, menu);
		mUpperItem = menu.findItem(R.id.explorer_toolbar_upper);
		mBackItem = menu.findItem(R.id.explorer_toolbar_back);
		mForwardItem = menu.findItem(R.id.explorer_toolbar_forward);
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
			case R.id.explorer_toolbar_upper:
				String upperPath;
				if (mCurrentPath.length() <= 3) // C:\
					upperPath = "";
				else
				{
					// C:\dire1\dire2 -> C:\dire1
					upperPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf("\\"));
					if (upperPath.length() <= 2) // C:\dire1 -> C:
						upperPath += "\\";       // C:\
				}
				goToPath(upperPath, true);
				break;
			case R.id.explorer_toolbar_back:
				if (!mBackStack.empty())
				{
					String currentPath = mCurrentPath;
					goToPath(mBackStack.pop(), false);
					mForwardStack.add(currentPath);
				}
				break;
			case R.id.explorer_toolbar_forward:
				if (!mForwardStack.empty())
				{
					String currentPath = mCurrentPath;
					goToPath(mForwardStack.pop(), false);
					mBackStack.add(currentPath);
				}
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
			mBackStack.add(mCurrentPath);
		mFileList.clear();
		int cursor = 0;
		if (isDisk)
		{
			mCurrentPath = "";
			while (cursor + 7 <= bytes.length)
			{
				String path = new String(bytes, cursor, 3);
				cursor += 3;
				int labelLength = Tools.getInt(bytes, cursor);
				cursor += 4;
				if (cursor + labelLength > bytes.length) break;
				String label = Tools.getString(bytes, cursor, labelLength);
				mFileList.add(new Disk(path, label));
				cursor += labelLength;
			}
		}
		else
		{
			int length = Tools.getInt(bytes, cursor);
			cursor += 4;
			mCurrentPath = Tools.getString(bytes, cursor, length);
			cursor += length;
			while (cursor + 8 <= bytes.length)
			{
				int attribute = Tools.getInt(bytes, cursor);
				length = Tools.getInt(bytes, cursor + 4);
				cursor += 8;
				if (cursor + length > bytes.length) break;
				String path = Tools.getString(bytes, cursor, length);
				mFileList.add(new FileSystemEntry(path, attribute));
				cursor += length;
			}
		}
		runOnUiThread(()->
		{
			mAdapter.changeSelectMode(false);
			mAdapter.notifyDataSetChanged();
			if (recognize) mForwardStack.clear();
			if (mUpperItem != null)
			{
				mUpperItem.setEnabled(!mCurrentPath.equals(""));
				mUpperItem.setIcon(mCurrentPath.equals("") ? R.drawable.ic_upper_disable : R.drawable.ic_upper);
			}
			if (mBackItem != null)
			{
				mBackItem.setEnabled(!mBackStack.empty());
				mBackItem.setIcon(mBackStack.empty() ? R.drawable.ic_back_disable : R.drawable.ic_back);
			}
			if (mForwardItem != null)
			{
				mForwardItem.setEnabled(!mForwardStack.empty());
				mForwardItem.setIcon(mForwardStack.empty() ? R.drawable.ic_forward_disable : R.drawable.ic_forward);
			}
			if (mFloatingMenu != null)
			{
				if (mCurrentPath.equals(""))
					mFloatingMenu.hideMenu(true);
				else
					mFloatingMenu.showMenu(true);
			}
			mNavigationFragment.changePath(mCurrentPath);
			mSwipeRefresh.setRefreshing(false);
			if (scrollToFirst)
				mRecyclerView.scrollToPosition(0);
		});
	}

	@Override
	public void onNavigationItemClick(String path)
	{
		goToPath(path, true);
	}

/*	private class MultiSelectMenuBar extends CustomMenuBar
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
	}*/

	private long backPressedTime;
	@Override
	public void onBackPressed()
	{
		if (mAdapter.isSelectMode())
			mAdapter.changeSelectMode(false);
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

