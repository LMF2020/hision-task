$(function() {

	jsPlumb.ready(function() {

	});

	var _all_start_btn = $('#start_site button')
	var _all_end_btn = $('#end_site button')

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

	// 前置任务
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
			sendCommand(command)
		})

	})

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

	// flag : 0-end; 1-recorver; 2-end;
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
		jsPlumb.connect({
			source: startPos[0],
			target: endPos[0],
			anchors: ["BottomRight", "TopLeft"],
			paintStyle: {
				strokeWidth: 1,
				stroke: '#346b41'
			},
			endpointStyle: {
				fill: 'rgb(243,229,0)'
			}
		});
	}
	
	var resetConnect = function(){
		jsPlumb.deleteEveryEndpoint ()
	}

})