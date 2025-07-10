<filename>AppiumXM-master/src/test/java/com/xiaoM/appium/utils/AppiumComm.java<fim_prefix>

package com.xiaoM.appium.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.xiaoM.Common.Utils.IOMananger;
import com.xiaoM.Common.Utils.TestListener;

public class AppiumComm {
	public static String adb = "adb";
	
	/**
	 * 获得应用的UID
	 * @param appPackage应用包名
	 * @return UID
	 * @throws IOException
	 */
	public static String getMobileAppUid(String appPackage,String device) throws IOException {
		//CMD 上运行需要改为adb shell "dumpsys package com.netease.mail | grep userId="
		<fim_suffix>

		try {
			Process p = pb.start();
			String line = null;
			BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
			if ((line = bf.readLine()) != null) {
				return line.split("=")[1].split(" ")[0]; // 分割字符串
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 获取应用流量数据
	 * @param appPackage 应用包名
	 * @return 上下行流量数据
	 */
	public static void getMobileAppNet(String appPackage,String device,String driverName) throws IOException {
		String line = null;
		String[] splitArr = null;
		String uid = getMobileAppUid(appPackage,device);
		ProcessBuilder pb1 = new ProcessBuilder(adb, "-s",device,"shell", "cat /proc/net/xt_qtaguid/stats" + " | grep " + uid);
		try {
			Process p = pb1.start();
			BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
			int rx = 0;
			int st = 0;
			while ((line = bf.readLine()) != null) {
				splitArr = line.split(" ");
				for (int x = 0; x < splitArr.length; x++) {
					if (x == 5) {
						rx += Integer.parseInt(splitArr[x]);//下行数据
					}
					if (x == 7) {
						st += Integer.parseInt(splitArr[x]);
					}
				}
			}
			String workSpace = TestListener.ProjectPath+"/test-output/MonitorResoure/Net";
			IOMananger.saveToFile(workSpace, driverName, String.valueOf(rx));
			IOMananger.saveToFile(workSpace, driverName, String.valueOf(st));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 强制停止应用
	 * @param appPackage 包名
	 * @throws IOException
	 */
	public static void forceStop(String appPackage,String device) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(adb,"-s",device, "shell", "am force-stop " + appPackage);
		try {
			pb.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 清除缓存
	 * 
	 * @param appPackage
	 * @return
	 */
	public static void adbClearCache(String appPackage,String device) {
		ProcessBuilder pb1 = new ProcessBuilder(adb,"-s",device,"shell", "pm", "clear", appPackage);
		try {
			pb1.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 启动app
	 * @param appPackageActivity
	 * @param device
	 */
	public static void adbStartAPP(String appPackageActivity,String device) {
		ProcessBuilder pb1 = new ProcessBuilder(adb,"-s",device, "shell", "am", "start", "-n", appPackageActivity);
		try {
			pb1.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * app启动时延
	 * @param cpuList
	 * @return
	 */
	public static String appLuanchTime(String appPackageActivity,String device){
		ProcessBuilder pb1 = new ProcessBuilder(adb,"-s",device, "shell", "am", "start", "-W","-n", appPackageActivity);
		List<String> results = new ArrayList<String>();
		String Time =null;
		try {
			Process process =pb1.start();
			Scanner scanner = new Scanner(process.getInputStream());
			while (scanner.hasNextLine()) {
				results.add(scanner.nextLine().toString());
			}
			scanner.close();
			for(String result:results){
				if(result.contains("ThisTime:")){
					Time = result.split(": ")[1];
				}
			}
			return Time;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	public static int cpuMaxComp(List<Integer> cpuList)  {
		int cpuMax = 0;// 计算最大值
		int cpu;
		for (int i = 0; i < cpuList.size(); i++) {
			cpu = cpuList.get(i);
			if (cpu > cpuMax) {
				cpuMax = cpu;
			}
		}
		return cpuMax;
	}
	public static int cpuAvg(List<Integer> cpuList)  {
		int cpuAvg = 0;// 计算均值
		int cpuSum = 0;
		for(int cpu:cpuList){
			cpuSum = cpuSum + cpu;
		}
		cpuAvg = Integer.valueOf(cpuSum/cpuList.size());		
		return cpuAvg;
	}
	public static Double memMaxComp(List<Double> menList)  {
		double memMax = 0;
		double men;
		for (int i = 0; i < menList.size(); i++) {
			men = menList.get(i);
			if (men > memMax) {
				memMax = men;
			}
		}
		return memMax;
	}
	public static Double menAvg(List<Double> menList)  {
		Double menAvg = 0.0;// 计算均值
		Double menSum = 0.0;
		for(Double men:menList){
			menSum = menSum + men;
		}
		menAvg = menSum/menList.size();		
		return menAvg;
	}
	public static void main(String[] args) {
		System.out.println(appLuanchTime("com.netease.mail/com.netease.mobimail.activity.LaunchActivity","192.168.1.103:5555"));
	}
}
<fim_middle>