package com.hision.erp.test;

import com.hision.erp.bean.TaskStatus;

public class Test {
	public static void main(String[] args) {
		
		int a = 6; // 货架
		int b = 6; // 列数
		
		System.out.println(a%b);
		
		// if (a<=b) => row = 1
		// if (a>b && a%b == 0) => row = a/b
		// if (a>b && a%b !=0) => row =a/b + 1
	
		
		String str = " cmd=position;pause_stat=0;battery=0;error=0;x=18003;y=12483;a=268.923004;z=51922;gAlarm=1;speed=0;task=A1-B1.xml;veer_angle=0.000000;task_step=0;task_isfinished=0;task_error=0;walk_path_id=1�";
		
		TaskStatus.ofme(str);
	}
}
