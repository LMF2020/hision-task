package com.hision.erp.client;

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
import org.tio.client.intf.ClientAioHandler;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.Node;

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
	private ClientAioHandler messageClientAioHandler;

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
	 * @throws Exception
	 */
	public void connect() throws Exception {
		// 连接服务器
		Node serverNode = new Node(conf.get("io.socket.host"), conf.getInt("io.socket.port"));
		clientGroupContext = new ClientGroupContext(messageClientAioHandler, aioListener, reconnConf);
		clientGroupContext.setHeartbeatTimeout(Const.TIMEOUT);
		aioClient = new AioClient(clientGroupContext);
		clientChannelContext = aioClient.connect(serverNode);

		log.info("正在尝试连接服务器：" + conf.get("io.socket.host") + ":" + conf.getInt("io.socket.port"));
	}

	public synchronized Result send(String message) throws Exception {
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
			return Result.error("任务：" + taskStatus.getTask() + "正在执行，设备电量：" + taskStatus.getBattery() + "，任务状态： "
					+ taskStatus.getTask_isfinished());
		}

		MessagePacket packet = new MessagePacket();
		packet.setBody(message.getBytes(MessagePacket.CHARSET));
		Aio.send(clientChannelContext, packet);
		log.info("正在发送命令：调度命令发送成功：" + message);

		// TODO: 这里有一个bug： 用户发送第一个调度命令的之后，状态还没来及上报就接着触发下一个调度命令，这时候我不知道该如何阻止用户连续发送

		// 解决方案：

		return Result.success();
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
		// 如果缓存任务不存在，返回false
		TaskStatus taskStatus = Const.taskCache.get(Const.CURRENT_TASK);
		if (taskStatus == null) {
			return false;
		}
		// 如果缓存任务存在， 并且任务已经完成，返回false
		if (taskStatus.isTaskDone()) {
			return false;
		}

		// 其他情况，说明任务没有完成
		return true;

	}

}
