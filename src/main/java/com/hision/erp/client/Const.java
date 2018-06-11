package com.hision.erp.client;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.Mvcs;

import com.google.common.collect.Maps;
import com.hision.erp.bean.Task;
import com.hision.erp.bean.TaskSite;
import com.hision.erp.bean.TaskStatus;
import com.hision.erp.service.TaskService;
import com.hision.erp.service.impl.TaskServiceImpl;

public class Const {

	private static final Log log = Logs.get();

	public static final String SEND_CMD_PATTERN = "cmd=set task by name;name={CODE}.xml";
	public static final String END_CMD_PATTERN = "cmd=pause;pauseStat=1";
	public static final String REC_CMD_PATTERN = "cmd=pause;pauseStat=0";
	public static final String DEFAULT_DB_TASK_ID = "WCNMWCNMdde94b0e9e87f376efd80c30";

	public static final int TASK_TODO = 0;
	public static final int TASK_IN_PROCESS = 1;
	public static final int TASK_FINISHED = 2;

	// 缓存任务列表(后期采用LRU模型, 如果是多任务需要引入memcached)
	public static Map<String, TaskStatus> taskCache = Maps.newConcurrentMap();
	public static Map<String, String> taskIdMap = Maps.newConcurrentMap();
	// 当前正在执行的任务
	public static String CURRENT_TASK = "";
	public static boolean IS_FIRST_SENT = true;

	private static TaskService taskService = Mvcs.ctx().getDefaultIoc().get(TaskServiceImpl.class);
	private static MessageClient messageClient = Mvcs.ctx().getDefaultIoc().get(MessageClient.class);

	/**
	 * 心跳超时时间
	 */
	public static final int TIMEOUT = 5000;

	/**
	 * 发送的报文
	 */
	public static String createSendCommad(String reqCode) {
		int pos = reqCode.indexOf(".xml");
		if(pos != -1) {
			reqCode = reqCode.substring(0, pos);
		}
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
	public static TaskStatus updateTaskStatus(String respCode) {

		// Only for test
		/*
		 * TaskStatus bean = new TaskStatus(); bean.setTask("A0-B2.xml");
		 * bean.setTask_isfinished(1); updateSiteStatus(bean);
		 */
		// Only for test

		if (Strings.isBlank(respCode)) {
			log.error("返回空报文");
			return null;
		}
		if (!respCode.contains("cmd=") || !respCode.contains("battery=") || !respCode.contains("task_isfinished=")
				|| !respCode.contains("task=")) {
			log.error("报文解析错误=>format() error：" + respCode);
			return null;
		}

		// 解析任务报文
		TaskStatus me = null;
		try {
			me = TaskStatus.ofme(respCode);
		} catch (Exception e) {
			log.error("报文解析错误=>ofme() error：" + respCode);
			throw e;
		}
		String taskName = me.getTask(); // 比如: A1-B1.xml

		// 判断当前任务跟当前报文返回的是否是同一个任务
		boolean activeTask = taskCache.containsKey(taskName);

		// 如果是就更新当前任务状态
		if (activeTask) {
			log.info("报文解析：更新任务：" + taskName);
			taskCache.get(taskName).update(me);

		} else {
			// 如果不是,就把报文添加到缓存
			log.info("报文解析：添加任务：" + taskName);
			taskCache.put(taskName, me);
		}

		log.info("报文解析: 当前任务为=>" + taskName);

		// 更新缓存
		CURRENT_TASK = taskName;

		if (me.isTaskDone()) {

			// 更新库表：满仓
			updateSiteStatus(me);

			// 继续下个任务
			// TODO: 性能??
			try {
				// 任务结束延迟2s再查询
				Thread.currentThread();
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			findNextTaskAndSend();
		}

		return me;
	}

	// 更新库表：满仓
	private static synchronized void updateSiteStatus(TaskStatus me) {
		String task = me.getTask();
		if (Strings.isNotBlank(task) && task.contains("-") && task.contains(".xml")) {

			String site = getSite(task); // A1-B1.xml => B1
			Dao dao = Mvcs.ctx().getDefaultIoc().get(Dao.class);
			TaskSite bean = new TaskSite(site, 1);
			dao.update(bean);
		}

		// 数据库更新上个任务的状态
		String lastTaskId = taskIdMap.get(task);
		if (StringUtils.isNotBlank(lastTaskId)) {
			Task lastTask = new Task();
			lastTask.setId(lastTaskId);
			lastTask.setStatus(Const.TASK_FINISHED);
			try {
				taskService.update(lastTask);
			} catch (Exception e) {
				log.error("任务已完成,但是状态更新失败！" + lastTask);
			}
		}
	}

	private static String getSite(String taskName) {
		int st = taskName.indexOf("-");
		int ed = taskName.indexOf(".xml");
		return taskName.substring(st + 1, ed);
	}

	// 如果A1-B1执行完后，车子已经在A了.但是结束报文还是A1-B1，此时如果从数据库取的是B1-C1，那么必须要先执行0A-0B.xml为了让报文纠正回来
	// 如果0B-0A, 那么必须再执行0A-0C/0B/0D才能加BCD的后续列表任务

	// 两种工作模式：
	// 1. A到B/C/D/Z
	// 2. 先发0A到0B/0C/0D，让车子停在BCD点 ，然后方可加B/C/D之间调度任务列表
	// 上一个任务与下一个任务作比较，选取合适的命令发送

	// * 这个方法什么时候触发? 1. 任务完成时触发 2. 程序启动时触发???
	public static String findNextTaskAndSend() {
		// 定义下一个任务的指令名称
		String cmd = "";
		String id = "";
		String siteLast = CURRENT_TASK;
		Task task = null;
		try {
			// 查找下个任务
			task = taskService.selectNext();
			if (task != null) {
				id = task.getId();
				String nextSite = task.getName();
				if (StringUtils.isBlank(siteLast)) {
					return cmd = createSendCommad(nextSite);
				}

				// 下一个任务的起始站
				String startSiteNext = getStartSite(nextSite);
				// 上一个任务的起始站
				String startSiteLast = getStartSite(siteLast);
				String startSiteLastFull = getStartSiteFull(siteLast);
				// 上一个任务的终点站
				String destSiteLast = getDestSite(siteLast);
				// 如果上一个任务的终点站与下一个任务的起始站相同，可以继续发下个任务
				// -------------执行下一个任务的必要条件是

				// 逻辑开始
				if ("0A-0B.xml,0A-0C.xml,0A-0D.xml".contains(siteLast)) {
/*//					================================
//							0A-0B.xml
//							0A-0C.xml
//							0A-0D.xml
//							---------------
//							1.A1-B1
//							2.B1-C1
//							3.C1-D1
//							4.Z1-C1
*/					// 此时车子在BCD点
					if ("A,Z".contains(startSiteNext)) {
						// 1.A1-B1
						// 4.Z1-C1
						// 强制按钮
					} else if (startSiteNext.equals(destSiteLast)) {
						// 2.B1-C1
						cmd = createSendCommad(nextSite);
					} else if (!startSiteNext.equals(destSiteLast)) {
						// 3.C1-D1
						cmd = createSendCommad("0" + destSiteLast + "-" + "0" + startSiteNext);
					}
				} else if ("0B-0A.xml,0C-0A.xml,0D-0A.xml,0A-0A.xml".contains(siteLast)) {
/*//					================================
//							0B-0A.xml
//							0C-0A.xml
//							0D-0A.xml
//							0A-0A.xml
//							------------------
//							1.A1-B1
//							2.B1-C1
//							3.C1-D1
//							4.D1-Z1
//							5.Z1-C1
*/					if (startSiteNext.equals("B") || startSiteNext.equals("C") || startSiteNext.equals("D")) {
						// 强制按钮
						// 2.B1-C1
						// 3.C1-D1
						// 4.D1-Z1
					} else if ("A,Z".contains(startSiteNext)) {
						// 1.A1-B1
						// 5.Z1-C1
						cmd = createSendCommad(nextSite);
					}
				} else {
/*//					================================XXXXXXXXX
//							A1-B1
//							------
//							1.B1-C1
//							2.C1-D2
//							3.D1-C2
//							A2-A3
//							A2-B1
*/
					boolean isStartSiteLastWithA = "A,Z".contains(startSiteLast);
					boolean isStartSiteNextWithBCD = "B,C,D".contains(startSiteNext);
					if (isStartSiteLastWithA && isStartSiteNextWithBCD) {  // 方法对应XXXXXXXXX
						// 1.B1-C1
						// 2.C1-D2
						// 3.D1-C2
						// 强制按钮
					} else if (isStartSiteLastWithA) { // 方法对应XXXXXXXXX
						// 1.A2-A3
						// 2.A2-B1
						// 3.Z1-Z3
						cmd = createSendCommad(nextSite);
					} else if ("0B,0C,0D".contains(startSiteLastFull)) {
/*//						================================
//								0B-0C
//								------
//								1.A1-B1
//								2.Z1-C1
//								3.B1-C1
//								4.C1-D1
//								5.D1-Z1
*/						if ("A,Z".contains(startSiteNext)) {
							// 1.A1-B1
							// 2.Z1-C1
							// 强制按钮
						} else if (destSiteLast.equals(startSiteNext)) {
							// 3.B1-C1
							// 4.C1-D1
							// 5.D1-Z1
							cmd = createSendCommad(nextSite);
						} else if (!destSiteLast.equals(startSiteNext)) {
							cmd = createSendCommad("0" + destSiteLast + "-" + "0" + startSiteNext);
						}
					} else if ("B,C,D".contains(startSiteLast)) {
/*//						================================
//								B1-C1
//								-------
//								1.A1-B1
//								2.Z1-D1
//								3.B1-C1
//								4.C1-D1
//								5.D1-C2
*/						if (destSiteLast.equals(startSiteNext)) {
							// 3.B1-C1
							// 4.C1-D1
							// 5.D1-Z1
							cmd = createSendCommad(nextSite);
						} else if (!destSiteLast.equals(startSiteNext)) {
							cmd = createSendCommad("0" + destSiteLast + "-" + "0" + startSiteNext);
						}
					}
				}

				// 逻辑结束
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("任务选取失败");
		}

		// 发送任务
		try {
			if (!StringUtils.isBlank(cmd)) {
				// 发送任务
				messageClient.send(cmd);
				// 更新任务ID映射表
				taskIdMap.put(cmd + ".xml", id);
				// 更新任务状态为进行中
				task.setStatus(Const.TASK_IN_PROCESS);
				taskService.update(task);
				log.info("任务报文发送成功:" + cmd);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("任务选取后发送失败");
		}

		return cmd;
	}

	// A1-B1
	// B1-C1

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

	// 任务的起始站(ABCDZ)
	public static String getStartSite(String site) {
		char start = site.charAt(0);
		if (start == '0') {
			return String.valueOf(site.charAt(1));
		}
		return String.valueOf(start);
	}

	// 任务的终点站(ABCDZ)
	public static String getDestSite(String site) {
		String dest = site.substring(site.indexOf("-") + 1, site.indexOf("-") + 2);
		if (dest.equals("0")) {
			return site.substring(site.indexOf("-") + 2, site.indexOf("-") + 3);
		}
		return dest;
	}

	// 任务的起点站 0A,A1,0B,B1...
	private static String getStartSiteFull(String site) {
		return site.substring(0, 2);
	}

}
