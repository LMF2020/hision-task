package com.hision.erp.service;

import java.util.List;

import com.hision.erp.bean.Task;

/**
 * 待办任务列表服务
 * 
 * @author Jed
 *
 */
public interface TaskService {

	// 添加任务
	void add(String taskName) throws Exception;

	// 更新任务
	void update(Task task) throws Exception;

	// 移除任务
	void delete(List<String> taskIds) throws Exception;

	// 移除指定任务
	void deleteList(List<Task> tasks) throws Exception;

	// 查询全部任务
	List<Task> listAll() throws Exception;

	// 获取任务
	Task get(String id) throws Exception;

	// 清空任务
	void clearAll() throws Exception;

	// 清空已完成的任务
	void clearFinished() throws Exception;

	// 选取下一个任务
	Task selectNext() throws Exception;

	// 置顶任务
	void top(String id) throws Exception;
}
