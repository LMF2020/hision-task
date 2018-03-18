package com.hision.erp;

import java.nio.charset.Charset;

import org.nutz.boot.NbApp;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Encoding;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Ok;

import com.hision.erp.client.Const;
import com.hision.erp.client.MessageClient;

/**
 * Launcher Created by Jed on 2018/03/17.
 *
 */
@IocBean(create = "init", depose = "depose")
public class MainLauncher {

	private static final Log log = Logs.get();

	@Inject
	protected PropertiesProxy conf;

	@Inject
	protected MessageClient messageClient;
	//
	// @Inject
	// protected Dao dao;

	@At("/")
	@Ok("beetl:/index.html")
	public void index() {
	}

	public void init() {
		// 环境检查
		if (!Charset.defaultCharset().name().equalsIgnoreCase(Encoding.UTF8)) {
			log.warn("This project must run in UTF-8, pls add -Dfile.encoding=UTF-8 to JAVA_OPTS");
		}

		try {
			messageClient.connect();
			messageClient.send(conf.get("test.scheduled.task"));

		} catch (Exception e) {
			log.error("连接服务器失败: " + Const.getStackMsg(e));
		}

	}

	public void depose() {
	}

	public static void main(String[] args) throws Exception {
		new NbApp(MainLauncher.class).run();
	}

}
