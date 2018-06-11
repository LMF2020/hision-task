$(function() {

	layui.use(['layer', 'table'], function() {
		var layer = layui.layer;
		var table = layui.table;

		table.render({
			elem: '#tasklist',
			height: 800,
			url: COMJS.CTX_PATH + "/task/list",
			cols: [
				[{
						field: 'name',
						title: '任务',
						width: 80
					},
					{
						field: 'status',
						title: '状态',
						width: 80,
						templet: function(d) {
							var text = '待办';
							if(d.status == 1) {
								text = '进行中';
							} else if(d.status == 2) {
								text = '<span style="color: #c00;">已完成</span>';
							}
							return text;
						}
					},
					{
						title: '操作',
						align: 'center',
						toolbar: '#mytool'
					}
				]
			]
		});

		//监听工具条
		table.on('tool(tasklist)', function(obj) {
			var data = obj.data; //获得当前行数据
			var layEvent = obj.event; //获得 lay-event 对应的值（也可以是表头的 event 参数对应的值）

			// 置顶
			if(layEvent === 'top') {
				sendTaskAction("top", data.id);
			} else if(layEvent === 'del') { // 删除
				layer.confirm('确定删除该任务(' + data.name + ')吗？', function(index) {
					layer.close(index);
					sendTaskAction("delete", data.id);
				});
			}
		});

		// 置顶 or 删除
		function sendTaskAction(action, taskId) {
			$.ajax({
					method: "GET",
					url: COMJS.CTX_PATH + "/task/" + action + "/" + taskId
				})
				.done(function(resp) {
					if(resp.code == 1) {
						console.error(resp.msg);
					} else {
						table.reload('tasklist');
					}
				});
		}

		jsPlumb.ready(function() {});
		// 根据locationURL 初始化页面
		initPage()

		var full = '<a class="btn btn-outline-danger full" href="javascript:void(0)">设为满仓</a>';
		var empty = '<a class="btn btn-outline-primary empty" href="javascript:void(0)">设为空仓</a>';
		var _all_start_btn = $('#start_site .list-group-item')
		var _all_end_btn = $('#end_site .list-group-item')
		var setDefalutTask = function(val) {
			if(val) {
				$('#task').text(val)
			} else {
				$('#task').text('您当前尚未选择起始站点与目标站点')
			}
		}
		setDefalutTask()
		// 获取起始点选中项
		var getStartPos = function() {
			var selectedbtns = _all_start_btn.filter('.active')
			if(selectedbtns.length === 0) {
				return null
			}
			return $(selectedbtns[0])
		}
		// 获取结束点选中项
		var getEndPos = function() {
			var selectedbtns = _all_end_btn.filter('.active')
			if(selectedbtns.length === 0) {
				return null
			}
			return $(selectedbtns[0])
		}
		// 选择起始点
		_all_start_btn.click(function() {
			var startPos = $(this)
			var endPos = getEndPos()
			if(endPos) { // 如果目标站点有值的话
				var endText = endPos.text()
				var startText = startPos.text()
				if(startText == endText) {
					COMJS.alert("起始站点不能与目标站点相同")
					return
				}
				setDefalutTask(startText + '-' + endText)
				// 设置连线
				resetConnect()
				connectLine(startPos, endPos)
			}
			_all_start_btn.removeClass('active')
			startPos.addClass('active')
		})
		// 选择结束点
		_all_end_btn.click(function() {
			var endPos = $(this)
			if(endPos.hasClass('full')) {
				COMJS.alert("目标站点已满仓")
				return
			}
			var startPos = getStartPos()
			if(!startPos) {
				COMJS.alert("请选择起始站点")
				return
			}
			var startText = startPos.text()
			var endText = endPos.text()
			if(startText == endText) {
				COMJS.alert("目标站点不能与起始站点相同")
				return
			}
			// 选中结束站点
			_all_end_btn.removeClass('active')
			endPos.addClass('active')
			// 设置任务
			setDefalutTask(startText + '-' + endText)
			// 设置连线
			resetConnect()
			connectLine(startPos, endPos)
		})
		// 手工任务
		$("#pre_task button").click(function() {
			var me = $(this);
			var task = me.attr('id')
			var command = me.text()
			COMJS.confirm('您确认提交前置任务吗? AGV路线 →' + command, function() {
				sendCommand(task)
			})
		})
		// 重新选择
		$('#clear_task').click(function() {
			_all_start_btn.removeClass('active')
			_all_end_btn.removeClass('active')
			setDefalutTask()
			resetConnect()
		})
		// 结束任务
		$("#end_task").click(function() {
			COMJS.confirm('您确认要结束任务吗? 结束前请确保有任务在执行', function() {
				otherCommand(0)
			})
		})
		// 恢复任务
		$("#rec_task").click(function() {
			COMJS.confirm('您确认要恢复任务吗?', function() {
				otherCommand(1)
			})
		})
		// 系统恢复
		// 没有任务在运行，但是发送不了命令
		$("#reboot_task").click(function() {
			COMJS.confirm('您确认要恢复系统吗? ', function() {
				otherCommand(2)
			})
		})
		// 提交
		$('#submit_task').click(function() {
			var startPos = getStartPos()
			var endPos = getEndPos()
			if(!startPos || !endPos) {
				COMJS.alert('请选择→ 起始站点 → 目标站点')
				return
			}
			var startText = startPos.text()
			var endText = endPos.text()
			var command = startText + '-' + endText
			COMJS.confirm('您确定提交任务吗? <span class="bg-danger text-white">提交前请确保前置任务已经执行完毕</span> AGV路线→' + command, function() {
				submitTask(command)
			})
		})
		var submitTask = function(cmd) {
			$.ajax({
					method: "GET",
					url: COMJS.CTX_PATH + "/task/add/" + cmd
				})
				.done(function(resp) {
					if(resp.code == 1) {
						COMJS.error(resp.msg);
					} else {
						table.reload('tasklist');
						COMJS.success(resp.msg);
					}
				});
		}

		var sendCommand = function(cmd) {
			$.ajax({
					method: "GET",
					url: COMJS.CTX_PATH + "/task/sendCommand/" + cmd
				})
				.done(function(resp) {
					if(resp.code == 1) {
						COMJS.error(resp.msg);
					} else {
						COMJS.success(resp.msg);
					}
				});
		}

		// flag : 0-end; 1-recover; 2-end;
		var otherCommand = function(flag) {
			var cmd = "/task/endCommand"
			if(flag === 1) {
				cmd = "/task/recCommand"
			} else if(flag === 2) {
				cmd = "/task/resetCommand"
			}
			$.ajax({
					method: "GET",
					url: COMJS.CTX_PATH + cmd
				})
				.done(function(resp) {
					if(resp.code == 1) {
						COMJS.error(resp.msg);
					} else {
						COMJS.success(resp.msg);
					}
				});
		}
		var connectLine = function(startPos, endPos) {
//			jsPlumb.connect({
//				source: startPos[0],
//				target: endPos[0],
//				anchors: ["BottomRight", "TopLeft"],
//				paintStyle: {
//					strokeWidth: 1,
//					stroke: '#346b41'
//				},
//				endpointStyle: {
//					fill: 'rgb(243,229,0)'
//				}
//			});
		}
		var resetConnect = function() {
			// jsPlumb.deleteEveryEndpoint()
		}
		// 连线重绘
		//   $(".btn-link").click(function(e) {
		//         e.preventDefault()
		//         resetConnect()
		//         var endPos = getEndPos()
		//         var startPos = getStartPos()
		//         if(startPos && endPos) {
		//              connectLine(startPos, endPos)
		//         }
		//   })
		/**
		 * WebSocket stream
		 */
		function getWsURI() {
			var loc = window.location;
			var uri = "ws://" + loc.host;
			return uri.slice(0, uri.lastIndexOf(":")) + ":9321"
		}

		function connect() {
			var ws_uri = getWsURI()
			var ws = new WebSocket(ws_uri);
			ws.onopen = function() {
				console.log('WebSocket is connected')
			};
			ws.onmessage = function(e) {
				// console.log('Message:', e.data);
				// 实时更新数据状态
				var data = JSON.parse(e.data)
				var name = data['task']
				var isfinished = data['task_isfinished']
				var battery = data['battery']
				var error = data['task_error']
				$("#current_task").text(name)
				$("#current_status").text(isfinished == 1 ? '已停止' : '进行中')
				$("#current_battery").text(battery + '%')
			};
			ws.onclose = function(e) {
				console.log('Socket is closed. Reconnect will be attempted in 1 second.', e.reason);
				setTimeout(function() {
					connect();
				}, 30000);
			};
			ws.onerror = function(err) {
				console.error('Socket encountered error: ', err.message, 'Closing socket');
				ws.close();
			};
		}
		// WebSocket 连接
		connect()
		// 初始化仓库状态
		sendRequestForUpdateStatus()
		// 10s发一次请求，更新仓库状态
		setInterval(function() {
			try{
				sendRequestForUpdateStatus()
				table.reload('tasklist')
			}catch(e){
				console.log(e)
			}
		}, 10000);

		function sendRequestForUpdateStatus() {
			$.ajax({
					method: "GET",
					url: COMJS.CTX_PATH + "/task/refreshStatus"
				})
				.done(function(resp) {
					if(resp.code == 1) {
						console.error(resp.msg);
					} else {
						renderSites(resp['data'])
					}
				});
		}

		function renderSites(arr) {
			var len = arr.length;
			for(var i = 0; i < len; i++) {
				var site = arr[i];
				var name = site['name'];
				var status = site['status'];
				// 判断Id站点是否存在
				if($("#" + name).length > 0) {
					// 判断站点状态是满仓还是空仓
					if(status == 1) {
						$("#" + name).removeClass('full').addClass('full');
					} else {
						$("#" + name).removeClass('full');
					}
				} else {
					// 站点不存在，直接忽略，比如C1，或一体机
				}
			}
		}
		/**
		 * 修改仓库状态
		 *
		 */
		$("#change_state").click(function() {
			var conf = {
				htmUrl: '/rs/form/list.html', // 模板路径
				title: '站点库存状态管理',
				shadeClose: true, //点击遮罩关闭
				closeBtn: 1, // 不显示右上角关闭按钮
				area: ['1000px', '800px'],
				success: function(layero, index) {
					initTable()
				}
			}
			COMJS.readform(conf);
		})

		function initTable() {
			var $table = $('#table');
			$table.bootstrapTable({
				url: COMJS.CTX_PATH + "/task/refreshStatus",
				search: true,
				columns: [{
					field: 'name',
					title: '站点',
					align: 'center'
				}, {
					field: 'status',
					title: '仓库状态',
					align: 'center',
					formatter: function(value, row, index) {
						if(value == 0) {
							return '空仓'
						}
						return '<h5><span class="text-danger">满仓</span></h5>'
					}
				}, {
					field: 'operate',
					title: '操作',
					align: 'center',
					events: {
						'click .full': function(e, value, row, index) {
							// 设为满仓
							sendRequestSetSiteState(row['name'], 1, function() {
								// 刷新表格
								$table.bootstrapTable('refresh', {
									silent: true
								})
								// 刷新主页列表
								sendRequestForUpdateStatus()
							});
						},
						'click .empty': function(e, value, row, index) {
							// 设为空仓
							sendRequestSetSiteState(row['name'], 0, function() {
								// 刷新表格
								$table.bootstrapTable('refresh', {
									silent: true
								})
								// 刷新主页列表
								sendRequestForUpdateStatus()
							});
						}
					},
					formatter: operateFormatter
				}]
			});
		}

		function operateFormatter(value, row, index) {
			if(row['status'] == 0) {
				return full;
			}
			return empty;
		}

		function sendRequestSetSiteState(site, status, callback) {
			var status_text = status == 0 ? '空仓' : '满仓';
			var promote = '您确定要把' + site + '设为' + status_text + '吗?';
			COMJS.confirm(promote, function(index) {
				$.ajax({
						method: "POST",
						url: COMJS.CTX_PATH + "/task/updateStatus/" + site + '/' + status
					})
					.done(function(resp) {
						layer.close(index);
						if(resp.code == 1) {
							console.error(resp.msg);
							COMJS.alert('设置失败')
						} else {
							callback()
						}
					});
			})
		}
		// 页面初始化
		function initPage() {
			var loc = window.location;
			var path = loc.pathname;
			// console.log(loc)
			switch(path) {
				case "/A":
					switchSite('start', ['B', 'C', 'D', 'Z'], false);
					switchSite('end', ['A'], false);
					break;
				case "/B":
					switchSite('start', ['A', 'C', 'D', 'Z'], false);
					switchSite('end', ['B'], false);
					break;
				case "/C":
					switchSite('start', ['B', 'A', 'D', 'Z'], false);
					switchSite('end', ['C'], false);
					break;
				case "/D":
					switchSite('start', ['B', 'C', 'A', 'Z'], false);
					switchSite('end', ['D'], false);
					break;
				case "/Z":
					switchSite('start', ['B', 'C', 'D', 'A'], false);
					switchSite('end', ['Z'], false);
					break;
				default:
					console.log(path)
			}
		}

		function switchSite(prefix, siteList, isShow) {
			var len = siteList.length;
			for(var i = 0, len; i < len; i++) {
				if(isShow) {
					$("#" + prefix + '_' + siteList[i]).show()
				} else {
					$("#" + prefix + '_' + siteList[i]).hide()
				}
			}
		}

	});

})