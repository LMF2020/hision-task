package com.hision.erp;

import java.nio.charset.Charset;
import java.util.List;

import org.nutz.boot.NbApp;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.util.Daos;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Encoding;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.By;
import org.nutz.mvc.annotation.Filters;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.filter.CrossOriginFilter;

import com.hision.erp.bean.TaskSite;
import com.hision.erp.client.Const;
import com.hision.erp.client.MessageClient;
import com.hision.erp.util.Result;
import com.hision.erp.websocket.WsMsgHandler;
import com.hision.erp.websocket.WsMsgStarter;

/**
 * Launcher Created by Jed on 2018/03/17.
 *
 */
@IocBean(create = "init", depose = "depose")
public class MainLauncher {

	private static final Log log = Logs.get();

	private WsMsgStarter appStarter;

	@Inject
	protected PropertiesProxy conf;

	@Inject
	protected MessageClient messageClient;

	@Inject
	protected Dao dao;

	@At({ "/", "/A", "/B", "/C", "D", "Z" })
	@Ok("beetl:/index.html")
	public void index() {
	}

	public void init() {
		// 环境检查
		if (!Charset.defaultCharset().name().equalsIgnoreCase(Encoding.UTF8)) {
			log.warn("This project must run in UTF-8, pls add -Dfile.encoding=UTF-8 to JAVA_OPTS");
		}

		// 初始化数据表
		initDataSource();

		try {
			// 启动推送服务
			appStarter = new WsMsgStarter(Integer.parseInt(conf.get("io.ws.port")), new WsMsgHandler());
			appStarter.start();

			// 客户端连接服务器
			messageClient.connect(appStarter.getServerGroupContext());

		} catch (Exception e) {
			log.error("连接服务器失败: " + Const.getStackMsg(e));
		}

	}

	private void initDataSource() {

		// 环境检查
		if (!Charset.defaultCharset().name().equalsIgnoreCase(Encoding.UTF8)) {
			log.warn("This project must run in UTF-8, pls add -Dfile.encoding=UTF-8 to JAVA_OPTS");
		}
		Daos.createTablesInPackage(dao, "com.hision.erp.bean", false);
		Daos.migration(dao, "com.hision.erp.bean", true, false);

		// 初始化表数据：A1-A4，B1-B4，C1-C4，D1-D4，Z1-Z4
		for (int i = 1; i <= 4; i++) {
			TaskSite siteA = new TaskSite("A" + i, 0);
			TaskSite siteB = new TaskSite("B" + i, 0);
			TaskSite siteC = new TaskSite("C" + i, 0);
			TaskSite siteD = new TaskSite("D" + i, 0);
			TaskSite siteZ = new TaskSite("Z" + i, 0);

			int siteA_ct = dao.count(TaskSite.class, Cnd.where("name", "=", "A" + i));
			int siteB_ct = dao.count(TaskSite.class, Cnd.where("name", "=", "B" + i));
			int siteC_ct = dao.count(TaskSite.class, Cnd.where("name", "=", "C" + i));
			int siteD_ct = dao.count(TaskSite.class, Cnd.where("name", "=", "D" + i));
			int siteZ_ct = dao.count(TaskSite.class, Cnd.where("name", "=", "Z" + i));

			if (siteA_ct == 0) {
				dao.insert(siteA);
			}
			if (siteB_ct == 0) {
				dao.insert(siteB);
			}
			if (siteC_ct == 0) {
				dao.insert(siteC);
			}
			if (siteD_ct == 0) {
				dao.insert(siteD);
			}
			if (siteZ_ct == 0) {
				dao.insert(siteZ);
			}
		}
	}

	@Filters(@By(type = CrossOriginFilter.class))
	@At("/task/sendCommand/?")
	@Ok("json")
	public Result sendCommand(String command) {
		try {
			return messageClient.send(command);
		} catch (Exception e) {
			return Result.error(e.getMessage());
		}
	}

	@Filters(@By(type = CrossOriginFilter.class))
	@At("/task/endCommand")
	@Ok("json")
	public Result endCommand() {
		try {
			return messageClient.end();
		} catch (Exception e) {
			return Result.error(e.getMessage());
		}
	}

	@Filters(@By(type = CrossOriginFilter.class))
	@At("/task/recCommand")
	@Ok("json")
	public Result recCommand() {
		try {
			return messageClient.recover();
		} catch (Exception e) {
			return Result.error(e.getMessage());
		}
	}

	@Filters(@By(type = CrossOriginFilter.class))
	@At("/task/resetCommand")
	@Ok("json")
	public Result resetCommand() {
		try {
			return messageClient.reset();
		} catch (Exception e) {
			return Result.error(e.getMessage());
		}
	}

	@Filters(@By(type = CrossOriginFilter.class))
	@At("/task/refreshStatus")
	@Ok("json")
	public Result refreshStatus() {
		try {
			List<TaskSite> beanList = dao.query(TaskSite.class, Cnd.NEW());
			return Result.success("", beanList);
		} catch (Exception e) {
			return Result.error(e.getMessage());
		}
	}

	@Filters(@By(type = CrossOriginFilter.class))
	@At("/task/updateStatus/?/?")
	@Ok("json")
	public Result updateStatus(String siteName, int status) {
		try {
			TaskSite site = new TaskSite(siteName, status);
			int flag = dao.update(site);
			if (flag == 1) {
				return Result.success();
			} else {
				return Result.error();
			}
		} catch (Exception e) {
			return Result.error(e.getMessage());
		}
	}

	public void depose() {

	}

	public static void main(String[] args) throws Exception {
		new NbApp(MainLauncher.class).setPrintProcDoc(false).run();
	}

}
