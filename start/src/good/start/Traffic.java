package good.start;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class Traffic extends Service {

	/*
	 * 
	 * 这里采用了android系统提供的sharedpreferences 来进行XML文件的存取工作，是代码变得简洁了起来
	 * 
	 * 设计了两个流量记录XML文件， 一个是total。这个文件主要用来记录整体的流量情况，包括每月每日的总流量，以及所有的总的流量
	 * 一个是single。这个文件主要用来记录每一个活动进程的流量情况
	 * 
	 * 两个XML的格式设计如下：
	 * 
	 * total需要记录的几个数据的关键字可以随意设计，然后在界面UI中采用对应的关键字取出即可
	 * single文件的流量记录需要用对应的进程名字当作关键字来设计，这样方便在UI和后台service中 分别进行读取，而不出现错误。
	 * 具体的实现参见下面的具体注释。
	 * 
	 * 
	 * 同时关于流量的统计信息可以由android提供的类方法来得到 这样就不用自己再去读文件了。
	 * 
	 * 然后存储在上面已经说了，所以也不用自己存文件自己来解析数据了。 省掉了两个写入读出文件的接口方法。
	 */

	/* 总的流量信息文件 */
	final private String TOTAL = "total";
	/* 每个活动流量信息文件 */
	final private String SINGLE = "single";
	/* 上一次读取的内容信息 */
	final private String LAST = "last";// 记录上次与这次的差值。

	private Handler objHandler = new Handler(); // 多线程

	private int mMonth;
	private int mDay;

	private long allTheRxTraffic; // 总的接收流量
	private long allTheTxTraffic; // 总的发送流量
	private long theGprsRxTraffic; // GPRS接收流量
	private long theGprsTxTraffic; // GPRS发送流量

	private Runnable mTasks = new Runnable() {
		public void run()// 运行该服务执行此函数
		{
			refresh();
			objHandler.postDelayed(mTasks, 30000);// 每3000毫秒执行一次
		}
	};

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		objHandler.postDelayed(mTasks, 0);
		System.out.println("ok the onstart start the mtasks.");
		super.onStart(intent, startId);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub

		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub

		return null;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		objHandler.removeCallbacks(mTasks);
		super.onDestroy();
	}

	private void refresh() {
		/*
		 * 第一步： 得到时间 第二步： 得到流量统计情况 第三步： 根据需求更新SP中的流量记录情况 其中需要 1. 更新每天的流量统计数据 2.
		 * 更新每个月的流量统计数据 如果到了新的一天或者新的一月，那么需要清空原先的数据 第四步： 循环更新数据，提供给界面UI程序使用
		 */

		System.out.println("resfresh \n");
		final Calendar c = Calendar.getInstance();
		mMonth = c.get(Calendar.MONTH) + 1;// 获取当前月份
		mDay = c.get(Calendar.DAY_OF_MONTH);// 获得天数
		// 第一步得到当前时间完成
		System.out.println(mMonth + "-" + mDay);

		allTheTxTraffic = android.net.TrafficStats.getTotalTxBytes();
		allTheRxTraffic = android.net.TrafficStats.getTotalRxBytes();

		theGprsTxTraffic = android.net.TrafficStats.getMobileTxBytes();
		theGprsRxTraffic = android.net.TrafficStats.getMobileRxBytes();

		System.out.println("the total:" + allTheRxTraffic + "-"
				+ allTheTxTraffic + "-" + theGprsRxTraffic + "-"
				+ theGprsTxTraffic);
		// 通过提供的类库直接读出来，挺好。
		// what's next
		// 自然是要记录到文件里面了。
		// 第二步得到流量统计情况完成

		// 获取SharedPreferences对象
		Context ctx = Traffic.this;
		SharedPreferences sp_last = ctx
				.getSharedPreferences(LAST, MODE_PRIVATE);
		// 首先应该取出上次存储的数据。
		Long oldAllTx_change = allTheTxTraffic
				- sp_last.getLong("allTheTxTraffic", 0);
		Long oldAllRx_change = allTheRxTraffic
				- sp_last.getLong("allTheRxTraffic", 0);
		Long gprsTx_change = theGprsTxTraffic
				- sp_last.getLong("theGprsTxTraffic", 0);
		Long gprsRx_change = theGprsRxTraffic
				- sp_last.getLong("theGprsRxTraffic", 0);

		System.out.println("总体流量change:" + oldAllRx_change + "-"
				+ oldAllTx_change + "-" + gprsRx_change + "-" + gprsTx_change);

		// 更新last里面的内容
		Editor editor = sp_last.edit();
		editor.putLong("allTheTxTraffic", allTheTxTraffic); // 存入整個的流量情況
		editor.putLong("allTheRxTraffic", allTheRxTraffic); // 包括四個信息
		editor.putLong("theGprsTxTraffic", theGprsTxTraffic);
		editor.putLong("theGprsRxTraffic", theGprsRxTraffic);
		editor.commit();

		// 更新total文件里面的内容，这样就更新了整个流量信息的统计。
		addTotal(mMonth, mDay, oldAllTx_change, oldAllRx_change, gprsTx_change,
				gprsRx_change);

		/********************************************************************************************/
		// 华丽的分割线。
		// 下面是对每个进程的数据进行统计了。this is just so complicated
		/*
		 * 第一步：获得当前正在使用的进程
		 * 
		 * 第二步：对每个进程的流量进行获取
		 * 
		 * 第三步：对每个进程的XML数据进行更新
		 * 
		 * 问题： 这么做会不会很慢呢？唉。
		 */

		// 这个由这个函数功能来实现
		addSingle(mMonth, mDay);

		// 每个进程的信息更新完毕，下面只需要设置好每次更新的时间，不能太长也不能太短
		// 流量监控本来就是允许误差的。
		// 不要太苛刻。
		// 最后得估计花个好几个小时进行真机测试。

	}

	private void addTotal(int month, int day, long a, long b, long c, long d) {
		/***************************************************************************************/
		// System.out.println("enter total");
		// 获取SharedPreferences对象
		Context ctx = Traffic.this;
		SharedPreferences sp_total = ctx.getSharedPreferences(TOTAL,
				MODE_PRIVATE);
		// 首先应该取出上次存储的数据。
		int oldMonth = sp_total.getInt("month", 0);
		int oldDay = sp_total.getInt("day", 0);
		Long oldAllTx = sp_total.getLong("day_Tx", 0);
		Long oldAllRx = sp_total.getLong("day_Rx", 0);
		Long gprsTx = sp_total.getLong("day_gprsTx", 0);
		Long gprsRx = sp_total.getLong("day_gprsRx", 0);
		// 下面需要更新天数据——————————————————————————————————————————————————————————————————
		if (oldDay != day) { // 如果不是同一天的信息
			oldAllTx = (long) 0;
			oldAllRx = (long) 0;
			gprsTx = (long) 0;
			gprsRx = (long) 0;
		}
		Editor editor = sp_total.edit();
		editor.putInt("month", month);
		editor.putInt("day", day); // 存入新的日期数据
		editor.putLong("day_Tx", oldAllTx + a); // 存入""整個的流量情況
		editor.putLong("day_Rx", oldAllRx + b); // 包括四個信息
		editor.putLong("day_gprsTx", gprsTx + c);
		editor.putLong("day_gprsRx", gprsRx + d);
		editor.commit();
		// 更新完毕。不要忘记了提交commit。

		// 下面需要更新月数据————————————————————————————————————————————————————————————————————
		oldAllTx = sp_total.getLong("month_Tx", 0);
		oldAllRx = sp_total.getLong("month_Rx", 0);
		gprsTx = sp_total.getLong("month_gprsTx", 0);
		gprsRx = sp_total.getLong("month_gprsRx", 0);
		if (oldMonth != month) {
			oldAllTx = (long) 0;
			oldAllRx = (long) 0;
			gprsTx = (long) 0;
			gprsRx = (long) 0;
		}
		editor.putLong("month_Tx", oldAllTx + a); // 存入每个月整個的流量情況
		editor.putLong("month_Rx", oldAllRx + b); // 包括四個信息
		editor.putLong("month_gprsTx", gprsTx + c);
		editor.putLong("month_gprsRx", gprsRx + d);
		editor.commit();
		// 日和月的数据更新完毕。
	}

	private void addSingle(int temp_month, int temp_day) {
		/***************************************************************************************/
		// 获取完毕。首先应该获得当前运行的进程列表。
		// 应该封装一个关于进程的类来做的
		// 好的，那就封装吧。
		// 等整个程序能跑起来了再按架构的思想来审查一次。
		// 同时对程序整个的外观和界面进行进一步的美化。
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		result = getCurrentProcess();
		// 获取了正在运行的进程列表，然后下面就是要根据这些返回来的ID和名称来更新数据了，更新数据可以
		// 采用统一的接口函数来实现，主要需要设计自己的键值对。
		changeSingle(result, temp_month, temp_day);
		// 对每个进程进行更改，这里貌似就是有点多余。
		// 其实是可以直接在getCurrentProcess里面实现的

		// change完毕之后就没有东西了，整个过程基本就圆满了，下面就只是需要各种完善了
		// 但是还得测试到底能否运行起来，可以把开机启动的消息换成别的广播来试试，
		// 主要是为了测试Traffic里面的功能都好不好用
		// OK to be continued
	}

	private void changeSingle(List<Map<String, Object>> temp, int temp_month,
			int temp_day) {
		// 首先取出总的进程流量信息，以及上次的流量信息。
		// 然后更新。
		//
		// 获取SharedPreferences对象
		Context ctx = Traffic.this;
		SharedPreferences sp_last = ctx
				.getSharedPreferences(LAST, MODE_PRIVATE);
		// 首先得到上次保存的信息，这里对每个进程需要一个循环的操作才行，循环的求出差值
		// 然后根据不同的日期关系进行增量的实现
		// 这个需要仔细写，防止出错

		for (int i = 0; i < temp.size(); i++) {
			Long last_single = sp_last.getLong(
					temp.get(i).get("process_appname") + "--single_last", 0); // 上次监控的流量数据
			Long new_single_Tx = android.net.TrafficStats
					.getUidTxBytes((Integer) (temp.get(i).get("process_uid")));
			Long new_single_Rx = android.net.TrafficStats
					.getUidRxBytes((Integer) (temp.get(i).get("process_uid")));

			// 得出增量
			Long total = new_single_Tx + new_single_Rx;
			Long change = total - last_single;

			// 这里需要记住更新上次监控的数据
			Editor editor = sp_last.edit();
			editor.putLong(
					temp.get(i).get("process_appname") + "--single_last", total);
			editor.commit();
			// 更新完毕。

			// 获取SharedPreferences对象
			SharedPreferences sp_single = ctx.getSharedPreferences(SINGLE,
					MODE_PRIVATE);
			// 取出已经存储的数据
			int month = sp_single.getInt("single_month", temp_month);
			int day = sp_single.getInt("single_day", temp_day);

			Long single_day = sp_single.getLong(
					temp.get(i).get("process_appname") + "--single_day", 0); // 上次监控的天流量数据

			if (temp_day != day) {
				// 第一次记录流量情况，或者是新的一天开始了。
				single_day = (long) 0;
			}

			// 还是同一天的信息，因此只需要累加上进程的流量增量即可。
			// 下面取出并且累加上change即可
			Editor editor1 = sp_single.edit();
			editor1.putInt("month", month);
			editor1.putInt("day", day); // 存入新的日期数据
			editor1.putLong(
					temp.get(i).get("process_appname") + "--single_day",
					single_day + change);
			editor1.commit();

			// 下面就是每个月的流量统计啦
			Long single_month;
			single_month = sp_single.getLong(temp.get(i).get("process_appname")
					+ "--single_month", 0); // 上次监控的月发送数据
			if (temp_month != month) { // 第一次记录流量情况，或者是新的一月开始了.
				single_month = (long) 0;
			} // 还是同一个月的信息，因此只需要累加上进程的流量增量即可。
			Editor editor2 = sp_single.edit();
			editor2.putLong(temp.get(i).get("process_appname")
					+ "--single_month", single_month + change);
			editor2.commit();
			// OK 这就是月数据更新完毕了。下面就是取出来进行显示的问题了。
			// 在StartActivity之中。貌似真机测试是好使的。
			// 但是模拟器上不好用。good luck
		}
	}

	private List<Map<String, Object>> getCurrentProcess() {
		// 正在运行的进程列表
		List<RunningAppProcessInfo> procList = new ArrayList<RunningAppProcessInfo>();

		// 更新进程列表
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		procList = activityManager.getRunningAppProcesses();

		List<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();

		for (Iterator<RunningAppProcessInfo> iterator = procList.iterator(); iterator
				.hasNext();) {
			RunningAppProcessInfo procInfo = iterator.next();
			HashMap<String, Object> map = new HashMap<String, Object>();
			// map.put("process_name", procInfo.processName); // add proc_name
			map.put("process_uid", procInfo.uid); // add proc_id

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
			map.put("process_appname", appInfo.loadLabel(pm).toString()); // proc_appname
			temp.add(map);
		}
		// 上面是从进程管理器中获得的代码，这些代码主要用来得到进程的名称和进程的ID，然后由进程的ID来
		// 得到流量统计信息，然后根据统计信息来就更新文件中的情况，流量更新我们在addSingle中完成，这里
		// 我们就简单的返回正在运行进程列表即可。
		return temp;
	}
}