package com.hision.erp.client;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.ServerGroupContext;
import org.tio.utils.json.Json;
import org.tio.websocket.common.WsResponse;

import com.hision.erp.bean.TaskStatus;

@IocBean
public class MessageClientAioHandler implements ClientAioHandler {

	private static final Log log = Logs.get();
	private static MessagePacket heartbeatPacket = new MessagePacket();

	// 推送服务
	private ServerGroupContext wsGroupCtx;

	/**
	 * 接收端解码: 字节转换成可发送的消息
	 */
	@Override
	public Packet decode(ByteBuffer buffer, ChannelContext channelContext) throws AioDecodeException {

		MessagePacket imPacket = new MessagePacket();

		int bodyLength = buffer.limit();
		if (bodyLength > 0) {
			byte[] dst = new byte[bodyLength];
			buffer.get(dst);
			imPacket.setBody(dst);
		}

		return imPacket;
	}

	/**
	 * 发送端转码: 消息转换成可发送的字节
	 */
	@Override
	public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {

		MessagePacket imPacket = (MessagePacket) packet;
		byte[] body = imPacket.getBody();
		int bodyLen = 0;
		if (body != null) {
			bodyLen = body.length;
		}

		ByteBuffer buffer = ByteBuffer.allocate(bodyLen);
		buffer.order(groupContext.getByteOrder());

		// 写入消息体
		if (body != null) {
			buffer.put(body);
		}

		return buffer;
	}

	/**
	 * 客户端处理消息: 监听服务端发送过来的消息
	 */
	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {
		MessagePacket imPacket = (MessagePacket) packet;
		byte[] body = imPacket.getBody();
		if (body != null) {
			String respCode = new String(body, MessagePacket.CHARSET);
			// 实时更新服务器返回的报文
			TaskStatus taskStatus = Const.updateTaskStatus(respCode);
			log.info("收到服务器消息：" + respCode);
			// 推送给客户端
			messagePush(taskStatus);
		}
	}

	/**
	 * 如果返回null，框架层面则不会发心跳；如果返回非null，框架层面会定时发本方法返回的消息包
	 */
	@Override
	public Packet heartbeatPacket() {
		return heartbeatPacket;
	}

	public ServerGroupContext getWsGroupCtx() {
		return wsGroupCtx;
	}

	public void setWsGroupCtx(ServerGroupContext wsGroupCtx) {
		this.wsGroupCtx = wsGroupCtx;
	}

	// 后台消息推送
	public void messagePush(TaskStatus taskStatus) throws UnsupportedEncodingException {
		if (taskStatus != null) {
			WsResponse resp = WsResponse.fromText(Json.toJson(taskStatus), MessagePacket.CHARSET);
			Aio.sendToAll(wsGroupCtx, resp);
		}
	}

}
