package com.hision.erp.client;

import java.io.UnsupportedEncodingException;

import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.tio.client.AioClient;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupContext;
import org.tio.client.ReconnConf;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.Node;
import org.tio.server.ServerGroupContext;

import com.hision.erp.bean.TaskStatus;
import com.hision.erp.util.Result;

/**
 * 单例类: 多个浏览器共享一个socket连接服务器，类似一个用户绑定一个socket
 * 
 * @author Jed
 *
 */
@IocBean
public class MessageClient {

	private static final Log log = Logs.get();

	@Inject
	protected PropertiesProxy conf;

	// handler, 包括编码、解码、消息处理
	@Inject
	private MessageClientAioHandler messageClientAioHandler;

	// 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
	private ClientAioListener aioListener = null;

	// 断链后自动连接的，不想自动连接请设为null
	private static ReconnConf reconnConf = new ReconnConf(5000L);

	// 一组连接共用的上下文对象
	private ClientGroupContext clientGroupContext = null;

	private AioClient aioClient = null;
	private ClientChannelContext clientChannelContext = null;

	/**
	 * 程序启动后初始化连接
	 * 
	 * @throws Exception
	 */
	public void connect(ServerGroupContext serverGroupCtx) throws Exception {
		// 连接服务器
		messageClientAioHandler.setWsGroupCtx(serverGroupCtx);
		Node serverNode = new Node(conf.get("io.socket.host"), conf.getInt("io.socket.port"));
		clientGroupContext = new ClientGroupContext(messageClientAioHandler, aioListener, reconnConf);
		clientGroupContext.setHeartbeatTimeout(Const.TIMEOUT);
		aioClient = new AioClient(clientGroupContext);
		clientChannelContext = aioClient.connect(serverNode);

		log.info("正在尝试连接服务器：" + conf.get("io.socket.host") + ":" + conf.getInt("io.socket.port"));
	}

	/**
	 * 重置任务, 相当于重启服务器
	 * 
	 * @return
	 */
	public synchronized Result reset() {

		// 结束当前任务
		end();
		// 清空缓存任务
		Const.CURRENT_TASK = "";
		Const.taskCache.clear();

		return Result.success("系统恢复成功");
	}

	/**
	 * 恢复任务
	 * 
	 * @param message
	 * @return
	 */
	public synchronized Result recover() {
		// 组装待发送消息
		String message = Const.createRecCommad();
		if (clientChannelContext == null || clientChannelContext.isClosed()) {
			log.error("正在恢复命令：但服务器尚未连接成功");
			return Result.error("服务器连接失败");
		}
		// 有任务在执行才能恢复
//		if (!checkHasTaskUnfinished()) {
			// TaskStatus taskStatus = Const.getCurrentUnfinishedTask();

			MessagePacket packet = new MessagePacket();
			try {
				packet.setBody(message.getBytes(MessagePacket.CHARSET));
			} catch (UnsupportedEncodingException e) {
				return Result.error("恢复命令发送失败：" + e.getMessage());
			}
			Aio.bSend(clientChannelContext, packet);
			log.info("正在恢复命令：" + message);
			return Result.success("恢复命令发送成功!");
//		} else {
//			return Result.error("有任务在执行，没法执行恢复操作");
//		}
	}

	/**
	 * 结束任务
	 * 
	 * @param message
	 * @return
	 */
	public synchronized Result end() {
		// 组装待发送消息
		String message = Const.createEndCommad();
		if (clientChannelContext == null || clientChannelContext.isClosed()) {
			log.error("正在结束命令：但服务器尚未连接成功");
			return Result.error("服务器连接失败");
		}
		// 有任务在执行才能结束
		if (checkHasTaskUnfinished()) {
			// TaskStatus taskStatus = Const.getCurrentUnfinishedTask();

			MessagePacket packet = new MessagePacket();
			try {
				packet.setBody(message.getBytes(MessagePacket.CHARSET));
			} catch (UnsupportedEncodingException e) {
				return Result.error("结束命令发送失败，失败原因：" + e.getMessage());
			}
			Aio.bSend(clientChannelContext, packet);
			log.info("正在结束命令：：" + message);
			return Result.success("结束命令发送成功!");
		} else {
			return Result.error("没有任务在执行，没法执行结束操作");
		}
	}

	/**
	 * 发送任务
	 * 
	 * @param message
	 * @return
	 */
	public synchronized Result send(String message) {
		// 组装待发送消息
		message = Const.createSendCommad(message);
		// 发送消息
		if (clientChannelContext == null || clientChannelContext.isClosed()) {
			log.error("正在发送命令：但服务器尚未连接成功");
			return Result.error("服务器连接失败");
		}

		// 判断是否有任务正在执行
		if (checkHasTaskUnfinished()) {
			// 拿出正在执行的任务，返回给界面
			TaskStatus taskStatus = Const.getCurrentUnfinishedTask();
			log.error("正在发送命令：但是当前有任务正在执行，不能发送新任务：TASK： " + taskStatus.getTask() + "，CMD：" + taskStatus.getCmd());
			return Result.error("已有任务正在执行：" + taskStatus.getTask() + "，设备电量：" + taskStatus.getBattery() + "，当前状态： "
					+ taskStatus.getTask_isfinished());
		}

		MessagePacket packet = new MessagePacket();
		try {
			packet.setBody(message.getBytes(MessagePacket.CHARSET));
		} catch (UnsupportedEncodingException e) {
			return Result.error("任务发送失败，失败原因：" + e.getMessage());
		}
		Aio.bSend(clientChannelContext, packet);
		log.info("正在发送命令：命令发送成功：" + message);

		// TODO: 这里有一个bug： 用户发送第一个调度命令的之后，状态还没来及上报就接着触发下一个调度命令，这时候我不知道该如何阻止用户连续发送

		// 解决方案：

		return Result.success("命令发送成功");
	}

	/**
	 * 判断当前任务是否仍在运行
	 * 
	 * @return
	 */
	private boolean checkHasTaskUnfinished() {

		// 当前没有任务，返回false
		if (Strings.isBlank(Const.CURRENT_TASK)) {
			return false;
		}
		// 如果缓存里没有任务，返回false
		TaskStatus taskStatus = Const.taskCache.get(Const.CURRENT_TASK);
		if (taskStatus == null) {
			return false;
		}
		// 如果缓存里有任务， 并且任务已经完成，返回false
		if (taskStatus.isTaskDone()) {
			return false;
		}

		// 其他情况，说明任务没有完成
		return true;

	}

}
