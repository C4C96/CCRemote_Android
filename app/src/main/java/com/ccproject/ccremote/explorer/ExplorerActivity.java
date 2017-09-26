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
import android.text.InputFilter;
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

import java.io.ByteArrayOutputStream;
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

	private int mMenuId = R.menu.explorer_toolbar;
	private MenuItem mBackMenuItem, mForwardMenuItem, mUpperMenuItem;
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
		initDrawer();
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
			mMenuId = isSelectMode ? R.menu.explorer_multiselect : R.menu.explorer_toolbar;
			invalidateOptionsMenu();
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

	private void initDrawer()
	{
		mDrawerLayout = (DrawerLayout) findViewById(R.id.Explorer_Drawer);
		findViewById(R.id.explorer_desktop).setOnClickListener((v)->goToPath(DESKTOP, true));
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
			editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
			editText.setLines(1);
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
						mFloatingMenu.close(true);
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
		getMenuInflater().inflate(mMenuId, menu);
		mUpperMenuItem = menu.findItem(R.id.explorer_toolbar_upper);
		mBackMenuItem = menu.findItem(R.id.explorer_toolbar_back);
		mForwardMenuItem = menu.findItem(R.id.explorer_toolbar_forward);
		refreshMenuItems();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		for (FileSystemEntry file : mAdapter.getSelected())
			Tools.writeString(bytes, file.getPath());

		switch (item.getItemId())
		{
			case android.R.id.home:
				mDrawerLayout.openDrawer(GravityCompat.START);
				break;

			case R.id.explorer_toolbar_upper:
				if (!mCurrentPath.equals(""))
				{
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
				}
				break;
			case R.id.explorer_toolbar_back:
				if (!mBackStack.empty())
				{
					String currentPath = mCurrentPath;
					goToPath(mBackStack.pop(), false);
					if (mForwardStack.empty() || !mForwardStack.peek().equals(currentPath))
						mForwardStack.add(currentPath);
				}
				break;
			case R.id.explorer_toolbar_forward:
				if (!mForwardStack.empty())
				{
					String currentPath = mCurrentPath;
					goToPath(mForwardStack.pop(), false);
					if (mBackStack.empty() || !mBackStack.peek().equals(currentPath))
						mBackStack.add(currentPath);
				}
				break;

			case R.id.explorer_multiselect_copy:
				myApplication.mLocalServer.send(LocalServer.COPY_FILE, bytes.toByteArray());
				mAdapter.changeSelectMode(false);
				break;
			case R.id.explorer_multiselect_cut:
				myApplication.mLocalServer.send(LocalServer.CUT_FILE, bytes.toByteArray());
				mAdapter.changeSelectMode(false);
				break;
			case R.id.explorer_multiselect_delete:
				new AlertDialog.Builder(ExplorerActivity.this)
						.setTitle(R.string.Confirm)
						.setMessage(R.string.DeleteConfirmMsg)
						.setPositiveButton(R.string.Yes, (dialog, which)->
						{
							myApplication.mLocalServer.send(LocalServer.DELETE_FILE, bytes.toByteArray());
							mAdapter.changeSelectMode(false);
						})
						.setNegativeButton(R.string.No, null)
						.show();
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
		int cursor = 0;
		if (isDisk)
		{
			if (recognize && !mBackStack.peek().equals(mCurrentPath))
				mBackStack.add(mCurrentPath);
			mFileList.clear();
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
			if (length < 0)
				runOnUiThread(()->Toast.makeText(ExplorerActivity.this, R.string.NoSuchFolder, Toast.LENGTH_SHORT).show());
			else
			{
				if (recognize && (mBackStack.empty() || !mBackStack.peek().equals(mCurrentPath)))
					mBackStack.add(mCurrentPath);
				mFileList.clear();
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
		}
		runOnUiThread(()->
		{
			mAdapter.changeSelectMode(false);
			mAdapter.notifyDataSetChanged();
			if (recognize) mForwardStack.clear();
			refreshMenuItems();
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
			mDrawerLayout.closeDrawers();
		});
	}

	public void refreshMenuItems()
	{
		if (mUpperMenuItem != null)
		{
			mUpperMenuItem.setIcon(mCurrentPath.equals("") ? R.drawable.ic_upper_disable : R.drawable.ic_upper);
			mUpperMenuItem.setEnabled(!mCurrentPath.equals(""));
		}
		if (mBackMenuItem != null)
		{
			mBackMenuItem.setIcon(mBackStack.empty() ? R.drawable.ic_back_disable : R.drawable.ic_back);
			mBackMenuItem.setEnabled(!mBackStack.empty());
		}
		if (mForwardMenuItem != null)
		{
			mForwardMenuItem.setIcon(mForwardStack.empty() ? R.drawable.ic_forward_disable : R.drawable.ic_forward);
			mForwardMenuItem.setEnabled(!mForwardStack.empty());
		}
	}

	@Override
	public void onNavigationItemClick(String path)
	{
		goToPath(path, true);
	}

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

