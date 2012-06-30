package good.process;

import android.graphics.drawable.Drawable;

/*
 *定义了程序的基本信息
 *图标，名称，进程名称
 *ID是用来获取流量信息的。 
 *
 **/

public class ProcessInfo {

	private Drawable icon; 		// 程序图标
	private String programName; // 程序名称
	private String processName; // 进程名称
	private int processId; 		// 进程ID

	public Drawable getIcon() {
		return icon;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public String getProgramName() {
		return programName;
	}

	public void setProgramName(String programName) {
		this.programName = programName;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public int getProcessId() {
		return processId;
	}

	public void setProcessId(int processId) {
		this.processId = processId;
	}

}
