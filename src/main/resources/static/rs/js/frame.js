layui.config({
	base: '/rs/js/'
}).extend({
	// 加載自定義模塊
	CommJS: 'common'
})

layui.use(['jquery', 'layer', 'laytpl', 'form', 'table', 'upload', 'element', 'CommJS'], function() {

	var $ = layui.$,
		layer = layui.layer,
		form = layui.form,
		table = layui.table,
		element = layui.element,
		laytpl = layui.laytpl,
		upload = layui.upload,
		CommJS = layui.CommJS;



	// 根据物料查询所在货架
	$('#searchShelfViaMa').click(function(e){
		e.preventDefault();
		var conf = {
			htmUrl: CommJS.HTM_TPL.htm_searchShelfViaMa, // 模板路径
			title: false,
			shadeClose: true, //点击遮罩关闭
			closeBtn: 0, // 不显示右上角关闭按钮
			skin: 'layer-unvisiable', //自定义class
			area: ['550px', '80px'],
			success: function(layero, index) {
				form.on('submit(searchShelfViaMa)', function(data) {
					$('#searchShelfViaMa .lay-submit').addClass('layui-disabled')
					$.ajax({
							method: "GET",
							url: CommJS.CTX_PATH + "/material/searchShelfViaMa/" + data.field.code
						})
						.done(function(resp) {
							$('#searchShelfViaMa .lay-submit').removeClass('layui-disabled')
							if(resp.code == 1) {
								CommJS.error(resp.msg);
							} else {
								// 查询成功后后高亮货架
								console.log('查询成功',resp)
								if(resp.data.length == 0){
									// TODO: 没有判断物料是否存在
									CommJS.success('该物料不存在或者未上架');
								}else{
									showSelectedShelf(resp.data);
								}
								
							}
						});

					return false;
				});
			}
		}
		CommJS.readform(conf);
		
	})
	

	// 初始化桌面

	// 调试模式启用
	//	$.ajax({
	//		dataType: "json",
	//		url: 'test/desk.json',
	//		data: "",
	//		success: function(data) {
	//			inner_updateDesktop(data);
	//		}
	//	});
	// 服务端请求
//	$.ajax({
//			method: "GET",
//			url: CommJS.CTX_PATH + "/settings/init"
//		})
//		.done(function(resp) {
//			if(resp.code == 1) {
//				CommJS.error(resp.msg);
//			} else {
//				console.log("桌面数据更新: ", resp);
//				// 更新桌面
//				inner_updateDesktop({
//					rowList: resp.data
//				});
//				CommJS.alert('桌面数据更新成功<br>欢迎访问海天物料管理系统!<br>窗口15s后自动关闭');
//			}
//		});

});