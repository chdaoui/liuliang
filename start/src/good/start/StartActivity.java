package good.start;

import android.app.*;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TabHost.OnTabChangeListener;

import java.util.*;

/*
 * 
 * 整个程序流程：
 * 
 * 整个程序分为两个部分
 * 其中一部分为后台sevice，用来更新XML文件中存储的文件信息。
 * 另外一部分负责的是界面UI，与用户进行交互，根据用户的点击进行相应的反应。
 * 
 * 后台sevice更新的数据包括总的流量信息以及每个活动进程的流量信息。
 * 后台工作参见Traffic文件，其中它是开机自启动的，监听开机广播。详见bootReceiver
 * 
 * 前台UI提供的查询选择包括本月的流量信息，今天的流量信息，每个进程的流量信息，同时提供一些相应的用户设置选项
 * 它是一个TABhost，提供紧凑的用户界面
 * 详见StartActivity
 * 
 * */

/* 
 * 这个类的作用：
 * 
 * 1. 显示程序启动界面，根据需要设计相应的logo
 * 2. 同时历史流量使用情况，包括3G以及wifi，以及流量总额,以及各个进程的流量使用情况
 * 3. 显示当前可以操作的选项，比如设置选项，历史流量查看等
 * 4. 布局问题是关键，一定要适应不同的手机的屏幕
 * 
 */

public class StartActivity extends TabActivity {
	/** Called when the activity is first created. */

	private TabHost myTab;

	/************************************************************************/
	// the widgets in the first tab;
	// 第一个TAB里面的相关内容
	private ListView lv_tab1;
	private List<Map<String, Object>> lv_data1;
	private SimpleAdapter adapter;
	private TextView tv;
	private Button btn1_tab1;
	private Button btn2_tab1;// 两个按钮控件

	/************************************************************************/
	// the widgets in the second tab;
	// 第二个TAB里面的相关内容
	private ExpandableListView elv_tab2;
	private List<Map<String, String>> groupData;
	private List<List<Map<String, Object>>> childData;
	private Button btn1_tab2;
	private MyAdapter Exdapter;

	/************************************************************************/
	// the widgets in the third tab;
	// 第三个TAB里面的相关内容
	private TextView tv_tab3;
	private EditText et_tab3;
	private Button btn_tab3;
	private CheckBox cb;

	/************************************************************************/

	/* 总的流量信息文件 */
	final private String TOTAL = "total";
	/* 每个活动流量信息文件 */
	final private String SINGLE = "single";
	/* 关于程序的一些配置信息文件，主要存储包括流量限额以及流量开关等信息。或许后续可以继续添加一些信息 */
	final private String CONFIGURE = "configure";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// System.out.println("next --- init --- blind\n");
		Intent intent = new Intent(this, Traffic.class);
		startService(intent);
		Toast.makeText(getApplicationContext(), "OK .", 2000).show();
		Init();
		Blind();
		// refresh();
	}

	private void Blind() {

		mylistener listen = new mylistener();// 新建一个自己的监听器

		myTab.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String arg0) {
				// TODO Auto-generated method stub
				// Toast.makeText(getApplicationContext(), "change",
				// 3000).show();
				if (arg0.equals("tab1")) {
					fresh1();
				} else if (arg0.equals("tab2")) {
					fresh2();
				} else {
					fresh3();
				}
			}
		});

		lv_tab1.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				showInfo();
				return false;
			}
		});

		elv_tab2.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView arg0, View arg1,
					int arg2, int arg3, long arg4) {
				// TODO Auto-generated method stub
				// System.out.println("good on child click");
				getSingle(arg2, arg3);
				return false;
			}
		});

		// 下面为三个控件分别关联监听器
		btn_tab3.setOnClickListener(listen);
		btn1_tab1.setOnClickListener(listen);
		btn2_tab1.setOnClickListener(listen);
		btn1_tab2.setOnClickListener(listen);
		// 减少对象的使用，加快速度和减少用电量，这些是必须的。

		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				if (arg1) {
					Toast.makeText(getApplicationContext(), "流量报警开", 2000)
							.show();
				} else {
					Toast.makeText(getApplicationContext(), "流量报警关", 2000)
							.show();
				}
				// 这个状态也需要写入到配置文件之中。
				Context ctx = StartActivity.this;
				SharedPreferences sp_con = ctx.getSharedPreferences(CONFIGURE,
						MODE_PRIVATE);
				Editor editor = sp_con.edit();
				editor.putBoolean("switch", arg1);
				editor.commit();
				// 提交完毕就写入了配置文件之中。
			}
		});
	}

	class mylistener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v == btn_tab3) { // btn_tab3需要做的工作
				String temp = et_tab3.getText().toString();
				// 得到之后需要写入配置文件之中。
				tv_tab3.setText("本月流量限额为：" + temp + "M");
				Context ctx = StartActivity.this;
				SharedPreferences sp_con = ctx.getSharedPreferences(CONFIGURE,
						MODE_PRIVATE);
				Editor editor = sp_con.edit();
				editor.putString("stream", temp);
				editor.commit();
			} else if (v == btn1_tab1) {
				getData_tab1();
				adapter = new SimpleAdapter(StartActivity.this, lv_data1,
						R.layout.stream_layout, new String[] { "title",
								"upload", "download", "wifi", "g3", "img" },
						new int[] { R.id.title, R.id.upload, R.id.download,
								R.id.wifi, R.id.g3, R.id.img });
				lv_tab1.setAdapter(adapter);
			} else if (v == btn2_tab1) {
				new AlertDialog.Builder(StartActivity.this)
						.setTitle("退出程序？")
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
										finish();
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
										dialog.cancel();
									}
								}).show();
			} else if (v == btn1_tab2) {
				getData_tab2();
				// Exdapter = new MyAdapter(StartActivity.this);
				elv_tab2.setAdapter(Exdapter);
			} else {

			}
		}
	};

	private void fresh1() {
		// readfromfile
		// getData_tab1();
		// SimpleAdapter adapter = new SimpleAdapter(this, lv_data1,
		// R.layout.stream_layout, new String[] { "title", "upload",
		// "download", "wifi", "g3", "img" }, new int[] {
		// R.id.title, R.id.upload, R.id.download, R.id.wifi,
		// R.id.g3, R.id.img });
		// lv_tab1.setAdapter(adapter);
	}

	private void fresh2() {

	}

	private void fresh3() {

	}

	private void clear() {
		// 这个函数的功能很明白，就是清空XML文件中的数据
		// 注意清空的时候一定要两个都同时清空，用来保证数据的一致性。
		// 属于后续完善功能

	}

	private void showInfo() {
		new AlertDialog.Builder(this)
				.setTitle("清空该数据？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(getApplicationContext(), "清空", 2000)
								.show();
						clear();
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.cancel();
						Toast.makeText(getApplicationContext(), "back", 2000)
								.show();
					}
				}).show();
	}

	private void getSingle(int group, int child) {
		String appName = (String) childData.get(group).get(child)
				.get("process_appname");
		// 得到了程序名称，那么就可以根据程序名称在single里面取出相应的数据了。
		// 因为考虑到可能需要查看没有在运行的程序的流量使用情况，因此决定采用程序名称作为关键字。
		// 所以程序名最好别有冲突。

		// 获取SharedPreferences对象
		Context ctx = StartActivity.this;
		SharedPreferences sp_single = ctx.getSharedPreferences(SINGLE,
				MODE_PRIVATE);

		Long data1 = sp_single.getLong(appName + "--single_day", 0);
		String result = "";
		double total2;
		if (data1 > 1024 * 1024) {
			total2 = (double) (data1) / (1024.0 * 1024.0);// 这样得到了本月总流量。M为单位
			total2 = (int) (total2 * 100) / 100.0;// 保留两位小数
			result = result + total2 + "M";
		} else if (data1 > 1024) {
			total2 = (double) (data1) / 1024.0; // KB为单位
			total2 = (int) (total2 * 100) / 100.0;// 保留两位小数
			result = result + total2 + "KB";
		}else{
			total2 = data1;
			result = result + total2 + "B";
		}

		Long data2 = sp_single.getLong(appName + "--single_month", 0);
		String result1 = "";
		if (data2 > 1024 * 1024) {
			total2 = (double) (data2) / (1024.0 * 1024.0);// 这样得到了本月总流量。M为单位
			total2 = (int) (total2 * 100) / 100.0;// 保留两位小数
			result1 = result1 + total2 + "M";
		} else if (data2 > 1024) {
			total2 = (double) (data2) / 1024.0; // KB为单位
			total2 = (int) (total2 * 100) / 100.0;// 保留两位小数
			result1 = result1 + total2 + "KB";
		}else{
			total2 = data2;						//B为单位
			result1 = result1 + total2 + "B";
		}

		if (result.equals("")) {
			result = "0.0B";
		}
		if (result1.equals("")) {
			result1 = "0.0B";
		}
		new AlertDialog.Builder(this).setTitle("程序名：" + appName)
				.setMessage("今日流量总和：" + result + "\n本月流量总和：" + result1)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();

		// 之后就是对于显示的问题了，可以直接采用对话框的形式显示出来，
		// 比较的方便查看和明了。
	}

	private void Init() {
		myTab = this.getTabHost();
		LayoutInflater.from(this).inflate(R.layout.main,
				myTab.getTabContentView(), true);
		myTab.setBackgroundColor(Color.argb(150, 22, 70, 150));

		// System.out.println("here");
		// the first tab
		myTab.addTab(myTab
				.newTabSpec("tab1")
				.setIndicator("流量",
						getResources().getDrawable(R.drawable.ic_launcher))
				.setContent(R.id.tab1));
		// 找到tab1的控件
		lv_tab1 = (ListView) findViewById(R.id.listview);
		tv = (TextView) findViewById(R.id.total);
		btn1_tab1 = (Button) findViewById(R.id.num1);
		btn2_tab1 = (Button) findViewById(R.id.num2);

		getData_tab1();
		adapter = new SimpleAdapter(this, lv_data1, R.layout.stream_layout,
				new String[] { "title", "upload", "download", "wifi", "g3",
						"img" }, new int[] { R.id.title, R.id.upload,
						R.id.download, R.id.wifi, R.id.g3, R.id.img });
		lv_tab1.setAdapter(adapter);
		lv_tab1.setCacheColorHint(0);// 防止点击时发生黑屏
		// -----------------------------------------------------------------------------------
		// the second tab
		// get the widget
		elv_tab2 = (ExpandableListView) findViewById(R.id.elv);
		btn1_tab2 = (Button) findViewById(R.id.num3);
		// get the data
		getData_tab2();
		Exdapter = new MyAdapter(this);
		elv_tab2.setAdapter(Exdapter);
		// elv_tab2.setGroupIndicator(null);
		elv_tab2.setCacheColorHint(0);

		myTab.addTab(myTab
				.newTabSpec("tab2")
				.setIndicator("应用",
						getResources().getDrawable(R.drawable.ic_launcher))
				.setContent(R.id.tab2));

		// ------------------------------------------------------------------------------------
		// the third tab
		myTab.addTab(myTab
				.newTabSpec("tab3")
				.setIndicator("设置",
						getResources().getDrawable(R.drawable.ic_launcher))
				.setContent(R.id.tab3));
		tv_tab3 = (TextView) findViewById(R.id.text1); // 流量显示的控件
		Context ctx = StartActivity.this;
		SharedPreferences sp_con = ctx.getSharedPreferences(CONFIGURE,
				MODE_PRIVATE);
		String stream = sp_con.getString("stream", "未设置");
		tv_tab3.setText("本月流量限额为:" + stream + "M");
		et_tab3 = (EditText) findViewById(R.id.stream);// 流量输入的EditText
		btn_tab3 = (Button) findViewById(R.id.ok); // 确定按钮
		cb = (CheckBox) findViewById(R.id.check); // 流量报警开关
		// 具体的流量报警怎么实现还有待商榷。
		// 但是最好的是在通知栏里面显示一条信息。
	}

	private void getMonth() {
		// 获取SharedPreferences对象
		Context ctx = StartActivity.this;
		SharedPreferences sp_total = ctx.getSharedPreferences(TOTAL,
				MODE_PRIVATE);

		lv_data1 = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = new HashMap<String, Object>();

		Long temp1 = sp_total.getLong("month_Tx", 0);
		Long temp2 = sp_total.getLong("month_Rx", 0);
		double data1 = (double) temp1 / (1024.0 * 1024.0);
		data1 = (int) (data1 * 100) / 100.0;
		double data2 = (double) temp2 / (1024.0 * 1024.0);
		data2 = (int) (data2 * 100) / 100.0;
		double total1 = data1 + data2;
		// 上面是得到每个月存储的数据，然后经过相应的转换。
		// 然后再写出相应的结果。

		map.put("title", "本月已经使用流量" + total1 + "M.");
		map.put("upload", "其中发送" + data1 + "M.");
		map.put("download", "其中接收" + data2 + "M.");

		temp1 = sp_total.getLong("month_gprsTx", 0);
		temp2 = sp_total.getLong("month_gprsRx", 0);
		data1 = (double) temp1 / (1024.0 * 1024.0);
		data2 = (double) temp2 / (1024.0 * 1024.0);
		double total2 = data1 + data2;
		total2 = (int) (total2 * 100) / 100.0;
		map.put("g3", "本月使用3G 流量" + total2 + "M.");

		double total3 = total1 - total2;
		total3 = (int) (total3 * 100) / 100.0;
		map.put("wifi", "本月使用wifi流量" + total3 + "M.");
		map.put("img", R.drawable.ic_launcher);
		lv_data1.add(map);

	}

	private void getDay() {
		// 获取SharedPreferences对象
		Context ctx = StartActivity.this;
		SharedPreferences sp_total = ctx.getSharedPreferences(TOTAL,
				MODE_PRIVATE);

		Map<String, Object> map = new HashMap<String, Object>();
		Long temp1 = sp_total.getLong("day_Tx", 0);
		Long temp2 = sp_total.getLong("day_Rx", 0);
		double data1 = (double) temp1 / (1024.0 * 1024.0);
		data1 = (int) (data1 * 100) / 100.0;
		double data2 = (double) temp2 / (1024.0 * 1024.0);
		data2 = (int) (data2 * 100) / 100.0;
		double total1 = data1 + data2; // 得到了总流量了。

		map.put("title", "今日已经使用流量" + total1 + "M.");
		map.put("upload", "其中发送" + data1 + "M.");
		map.put("download", "其中接收" + data2 + "M.");

		temp1 = sp_total.getLong("day_gprsTx", 0);
		temp2 = sp_total.getLong("day_gprsRx", 0);
		data1 = (double) temp1 / (1024.0 * 1024.0);
		data2 = (double) temp2 / (1024.0 * 1024.0);
		double total2 = data1 + data2;
		total2 = (int) (total2 * 100) / 100.0;
		map.put("g3", "今日使用3G 流量" + total2 + "M.");

		double total3 = total1 - total2;
		total3 = (int) (total3 * 100) / 100.0;
		map.put("wifi", "今日使用wifi流量" + total3 + "M.");
		map.put("img", R.drawable.ic_launcher);

		lv_data1.add(map);
	}

	private void getData_tab1() {// read from file .
		Context ctx = StartActivity.this;
		SharedPreferences sp_con = ctx.getSharedPreferences(CONFIGURE,
				MODE_PRIVATE);
		String stream = sp_con.getString("stream", "未设置");
		tv.setText("本月流量限额为：" + stream + "M");

		getMonth(); // 获得每月流量使用情况
		/*********************************************************************************************************/
		getDay(); // 获得每天流量使用情况
	}

	private void getData_tab2() {// it's unique. can be programmed.

		/********************* get the groupData ******************************************/
		groupData = new ArrayList<Map<String, String>>();
		Map<String, String> temp = new HashMap<String, String>();
		Map<String, String> temp1 = new HashMap<String, String>();
		temp.put("text", "            正在运行的应用");
		temp1.put("text", "            所有的应用");
		groupData.add(temp);
		groupData.add(temp1);

		/********************* get the childData ---1 *********************************************/

		childData = new ArrayList<List<Map<String, Object>>>();

		// 整个基本过程
		// 第一步得到正在运行的进程信息，得到进程ID，进程的名字等信息。
		// 第二部由进程可以得到应用程序的包，有了包，那么就能得到应用程序信息。那么应用程序的名字，图标等一些东西就有了。
		// 然后将数据处理好就行了。

		// 正在运行的进程列表
		List<RunningAppProcessInfo> procList = new ArrayList<RunningAppProcessInfo>();

		// 更新进程列表
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		procList = activityManager.getRunningAppProcesses();

		List<Map<String, Object>> temp2 = new ArrayList<Map<String, Object>>();

		for (Iterator<RunningAppProcessInfo> iterator = procList.iterator(); iterator
				.hasNext();) {
			RunningAppProcessInfo procInfo = iterator.next();
			HashMap<String, Object> map = new HashMap<String, Object>();
			// 下面两个东西基本没有用到
			map.put("process_name", procInfo.processName); // add proc_name
			map.put("process_id", procInfo.pid + ""); // add proc_id

			// 包管理器
			PackageManager pm = getApplicationContext().getPackageManager();
			String[] pkgNameList = procInfo.pkgList; // 获得运行在该进程里的所有应用程序包
			String pkgName = pkgNameList[0];
			ApplicationInfo appInfo = null;
			try {
				appInfo = pm.getApplicationInfo(pkgName, 0);// 由包得到图标和应用程序名称等信息。
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			map.put("process_icon", (Drawable) appInfo.loadIcon(pm)); // proc_icon
			map.put("process_appname", appInfo.loadLabel(pm).toString()); // proc_appname
			try {
				PackageInfo packageInfo = pm.getPackageInfo(pkgName, 0);
				map.put("process_versionCode", packageInfo.versionName);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			temp2.add(map);
		}
		childData.add(temp2);

		/********************* get the childData ---2 *********************************************/

		List<Map<String, Object>> temp3 = new ArrayList<Map<String, Object>>();

		List<PackageInfo> packages = getPackageManager()
				.getInstalledPackages(0);
		for (int i = 0; i < packages.size(); i++) {
			PackageInfo packageInfo = packages.get(i);
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("process_appname",
					packageInfo.applicationInfo.loadLabel(getPackageManager())
							.toString());
			map.put("process_packageName", packageInfo.packageName);
			map.put("process_versionCode", packageInfo.versionName);
			map.put("process_icon",
					packageInfo.applicationInfo.loadIcon(getPackageManager()));
			// Only display the non-system app info
			temp3.add(map);
			/********************* done loading childData ---2 *********************************************/
		}
		childData.add(temp3);
	}

	class MyAdapter extends BaseExpandableListAdapter {
		Context context;
		LayoutInflater mlayoutInflater;

		MyAdapter(Context context) {
			this.context = context;
			mlayoutInflater = LayoutInflater.from(context);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return childData.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub

			convertView = mlayoutInflater.inflate(R.layout.child, null);

			TextView textView = (TextView) convertView
					.findViewById(R.id.childText);

			textView.setText("程序名："
					+ childData.get(groupPosition).get(childPosition)
							.get("process_appname").toString());

			TextView versionCode = (TextView) convertView
					.findViewById(R.id.code);

			versionCode.setText("版本："
					+ childData.get(groupPosition).get(childPosition)
							.get("process_versionCode").toString());

			ImageView image = (ImageView) convertView
					.findViewById(R.id.child_icon);

			image.setImageDrawable((Drawable) childData.get(groupPosition)
					.get(childPosition).get("process_icon"));

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return childData.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			// TODO Auto-generated method stub
			return groupData.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			// TODO Auto-generated method stub
			return groupData.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub

			convertView = mlayoutInflater.inflate(R.layout.group, null);

			TextView textParent = (TextView) convertView
					.findViewById(R.id.groupText);

			textParent.setText(groupData.get(groupPosition).get("text")
					.toString());
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return true;
		}
	}

}