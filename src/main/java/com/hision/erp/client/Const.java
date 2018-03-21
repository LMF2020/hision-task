package com.hision.erp.client;

import java.util.Map;

import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import com.google.common.collect.Maps;
import com.hision.erp.bean.TaskStatus;

public class Const {

	private static final Log log = Logs.get();

	public static final String SEND_CMD_PATTERN = "cmd=set task by name;name={CODE}.xml";
	public static final String END_CMD_PATTERN = "cmd=pause;pauseStat=1";
	public static final String REC_CMD_PATTERN = "cmd=pause;pauseStat=0";

	// 缓存任务列表(后期采用LRU模型, 如果是多任务需要引入memcached)
	public static Map<String, TaskStatus> taskCache = Maps.newConcurrentMap();
	// 当前正在执行的任务
	public static String CURRENT_TASK = "";
	public static boolean IS_FIRST_SENT = true;

	/**
	 * 心跳超时时间
	 */
	public static final int TIMEOUT = 5000;

	/**
	 * 发送的报文
	 */
	public static String createSendCommad(String reqCode) {
		return SEND_CMD_PATTERN.replace("{CODE}", reqCode);
	}

	/**
	 * 结束报文
	 */
	public static String createEndCommad() {
		return END_CMD_PATTERN;
	}

	/**
	 * 恢复报文
	 */
	public static String createRecCommad() {
		return REC_CMD_PATTERN;
	}

	/**
	 * 获取当前正在执行的任务，如果没有任务在执行，返回null
	 * 
	 * @return
	 */
	public static TaskStatus getCurrentUnfinishedTask() {
		if (!Strings.isBlank(CURRENT_TASK)) {
			TaskStatus taskStatus = Const.taskCache.get(CURRENT_TASK);
			if (!taskStatus.isTaskDone()) {
				return taskStatus;
			}
		}
		return null;
	}

	/**
	 * 解析接收的报文
	 * 
	 * @param respCode
	 * @return
	 */
	public static void updateTaskStatus(String respCode) {
		if (Strings.isBlank(respCode)) {
			log.error("返回报文：空报文");
			return;
		}
		if (!respCode.contains("cmd=") || !respCode.contains("battery=") || !respCode.contains("task_isfinished=")
				|| !respCode.contains("task=")) {
			log.error("返回报文格式错误：" + respCode);
			return;
		}

		// 解析任务报文
		TaskStatus me = null;
		try {
			me = TaskStatus.ofme(respCode);
		} catch (Exception e) {
			log.error("正在解析报文：报文解析任务出错：" + respCode);
			throw e;
		}
		String taskName = me.getTask();

		// 查找缓存是否存在该任务
		boolean activeTask = taskCache.containsKey(taskName);

		// 如果存在，更新任务状态
		if (activeTask) {
			log.info("正在解析报文：更新缓存任务：" + taskName);
			taskCache.get(taskName).update(me);

		} else {
			// 如果不存在,添加到缓存
			log.info("正在解析报文：添加新任务：" + taskName);
			taskCache.put(taskName, me);
		}

		log.info("正在解析报文: 设置当前正在执行的缓存任务：" + taskName);
		CURRENT_TASK = taskName;
	}

	/**
	 * 打印错误堆栈信息
	 * 
	 * @param e
	 * @return
	 */
	public static String getStackMsg(Throwable e) {

		StringBuffer sb = new StringBuffer();
		StackTraceElement[] stackArray = e.getStackTrace();
		for (int i = 0; i < stackArray.length; i++) {
			StackTraceElement element = stackArray[i];
			sb.append(element.toString() + "\n");
		}
		return sb.toString();
	}
}
