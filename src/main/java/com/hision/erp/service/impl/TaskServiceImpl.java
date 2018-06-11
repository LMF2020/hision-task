package com.hision.erp.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nutz.castor.castor.Collection2Array;
import org.nutz.castor.castor.Collection2String;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import com.hision.erp.bean.Task;
import com.hision.erp.client.Const;
import com.hision.erp.service.TaskService;

@IocBean
public class TaskServiceImpl implements TaskService {

	private static final Log LG = Logs.get();

	@Inject
	protected Dao dao;

	@Override
	public void update(Task task) throws Exception {
		if (StringUtils.isBlank(task.getId())) {
			throw new Exception("更新任务Id不存在");
		}
		dao.updateIgnoreNull(task);
		LG.info("任务:(" + task.getName() + ")更新成功, 状态为:" + task.getStatus());
	}

	@Override
	public void delete(List<String> taskIds) throws Exception {
		for (String id : taskIds) {
			dao.delete(Task.class, id);
		}
		LG.info("任务:(" + String.join(",", taskIds) + ")已被删除.");
	}

	@Override
	public void deleteList(List<Task> tasks) throws Exception {
		for (Task task : tasks) {
			dao.delete(task);
		}
	}

	/**
	 * 查询任务列表, 排序：已完成任务 > 进行中任务 > 待办任务
	 */
	@Override
	public List<Task> listAll() throws Exception {
		List<Task> taskList = dao.query(Task.class, Cnd.orderBy().asc("ordering"));
		if (taskList.isEmpty()) {
			return Lists.newArrayList();
		}
		List<Task> todoTaskList = Lists.newArrayList();
		List<Task> finishedTaskList = Lists.newArrayList();
		List<Task> inProgressTaskList = Lists.newArrayList();
		// 把任务分类
		for (Task task : taskList) {
			switch (task.getStatus()) {
			case Const.TASK_FINISHED:
				finishedTaskList.add(task);
				break;
			case Const.TASK_IN_PROCESS:
				inProgressTaskList.add(task);
				break;
			case Const.TASK_TODO:
				todoTaskList.add(task);
				break;
			default:
				break;
			}
		}

		inProgressTaskList.addAll(todoTaskList);
		finishedTaskList.addAll(inProgressTaskList);
		return finishedTaskList;
	}

	@Override
	public Task get(String id) throws Exception {
		return dao.fetch(Task.class, id);
	}

	@Override
	public void clearAll() throws Exception {
		dao.clear(Task.class);
	}

	/**
	 * 清空已完成的任务
	 */
	@Override
	public void clearFinished() throws Exception {
		dao.clear(Task.class, Cnd.where("status", "=", Const.TASK_FINISHED));
	}

	/**
	 * 新增任务
	 * 
	 */
	@Override
	public void add(String taskName) throws Exception {
		// 如果库表已经存在 A% 并且状态未完成的,不允许添加新任务
		List<Task> startAList = dao.query(Task.class,
				Cnd.where("name", "like", "A%").or("name", "like", "Z%").and("status", "!=", 2));
		if (!startAList.isEmpty()) {
			throw new Exception("起始点开始的任务在任务队列里有唯一性限制");
		}
		List<Task> todoTaskList = dao.query(Task.class, Cnd.orderBy().desc("ordering"));
		Task task = new Task();
		task.setStatus(Const.TASK_TODO);
		task.setName(taskName);
		if (todoTaskList.size() != 0) {
			Task lastTask = todoTaskList.get(0);
			// 如果任务列表有任务, 新增的任务序号要加一
			task.setOrdering(lastTask.getOrdering() + 1);
		}
		// 如果任务列表没有任务, 新增的任务序号为默认值50000
		dao.insert(task);
		LG.info("任务:(" + taskName + ")已被添加.");
	}

	/**
	 * 选择任务
	 * 
	 */
	@Override
	public Task selectNext() throws Exception {
		List<Task> todoTaskList = dao.query(Task.class, Cnd.where("status", "=", 0).orderBy("ordering", "asc"));
		if (todoTaskList.size() != 0) {
			return todoTaskList.get(0);
		}
		return null;
	}

	/**
	 * 任务置顶
	 * 
	 */
	@Override
	public void top(String id) throws Exception {
		List<Task> todoTaskList = dao.query(Task.class, Cnd.where("status", "=", 0).orderBy("ordering", "asc"));
		if (todoTaskList.size() != 0) {
			// 拿出当前顺序最靠前的任务
			Task topTask = todoTaskList.get(0);
			Task curTask = dao.fetch(Task.class, id);
			// 如果当前顺序最靠前的任务比我选择的任务靠前才会把我选择的任务置顶
			if (topTask.getOrdering() < curTask.getOrdering()) {
				// 置顶的方案就是把最靠前的顺序减一赋值给我选择的任务
				curTask.setOrdering(topTask.getOrdering() - 1);
				dao.update(curTask);
			}
		}
	}

}
