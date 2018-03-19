package com.hision.erp.bean;

/**
 * 任务状态
 * 
 * @author Jed
 *
 */
public class TaskStatus {

	private String cmd;
	private String task;
	private String battery;
	private int task_isfinished;
	private String task_error;

	/**
	 * 创建实时任务
	 * @param respCode
	 * @return
	 */
	public static TaskStatus ofme(String respCode) {
		
		TaskStatus me = new TaskStatus();
		String[] kv = respCode.trim().split(";");

		for (String item : kv) {
			String[] pair = item.split("=");
			String pair_k = pair[0].trim();
			String pair_v = pair[1].trim();

			if (pair_k.equals("cmd")) {
				me.setCmd(pair_v);
			} else if (pair_k.equals("task")) {
				me.setTask(pair_v);
			} else if (pair_k.equals("battery")) {
				me.setBattery(pair_v);
			} else if (pair_k.equals("task_isfinished")) {
				me.setTask_isfinished(Integer.valueOf(pair_v));
			} else if (pair_k.equals("task_error")) {
				me.setTask_error(pair_v);
			}
		}

		return me;
	}

	
	/**
	 * 更新实时任务状态
	 * @param me
	 */
	public void update(TaskStatus me) {
		
		this.setBattery(me.getBattery());
		this.setCmd(me.getCmd());
		this.setTask_error(me.getTask_error());
		this.setTask_isfinished(me.getTask_isfinished());
	}
	
	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getBattery() {
		return battery;
	}

	public void setBattery(String battery) {
		this.battery = battery;
	}

	public int getTask_isfinished() {
		return task_isfinished;
	}

	public void setTask_isfinished(int task_isfinished) {
		this.task_isfinished = task_isfinished;
	}
	
	public boolean isTaskDone() {
		return task_isfinished == 1;
	}

	public String getTask_error() {
		return task_error;
	}

	public void setTask_error(String task_error) {
		this.task_error = task_error;
	}

}
