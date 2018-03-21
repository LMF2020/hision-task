var COMJS = (function() {

	var _confirm = function(msg, myfunc) {
		msg = '<h5>' + msg + '</h5>'
		layer.confirm(msg, {
			title: '提示',
			btn: ['确定', '取消'],
			area: ['500px', '200px'],
			skin: 'demo-class',
			icon: 3,
			anim: 0
		}, function() {
			if(myfunc && $.isFunction(myfunc)) {
				myfunc()
			}
		}, function() {
			layer.close()
		})
	}

	var _alert = function(msg) {
		msg = '<h5>' + msg + '</h5>'
		layer.alert(msg, {
			skin: 'demo-class',
			area: ['500px', '200px'],
			icon: 6
		})
	}

	var _error = function(msg) {
		msg = '<h5>' + msg + '</h5>'
		layer.alert(msg, {
			icon: 2,
			area: ['500px', '250px'],
		})
	}

//	var _success = function(msg) {
//		msg = msg || '命令执行成功'
//		var msg = '<h5>' + msg + ',您是否需要关闭窗口？' + '</h5>'
//		layer.confirm(msg, {
//			time: 20000, //20s后自动关闭
//			btn: ['关闭', '继续'] //按钮
//		}, function(index) {
//			layer.closeAll()
//		}, function() {
//
//		})
//	}
	
	var _success = function(msg) {
		msg = msg || '命令执行成功'
		var msg = '<h5>' + msg + '</h5>'
		layer.alert(msg, {
			icon: 1,
			area: ['500px', '250px'],
		})
	}

	return {
		CTX_PATH: '',
		confirm: _confirm,
		alert: _alert,
		success: _success,
		error: _error
	}
})()