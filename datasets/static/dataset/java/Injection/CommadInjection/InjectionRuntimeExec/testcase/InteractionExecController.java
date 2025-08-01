<filename>0x7eTeamTools-main/src/main/java/com/sec421/controller/postPentest/InteractionExecController.java<fim_prefix>

package com.sec421.controller.postPentest;
/**
 * @author 0x421
 * @date 2023/12/28 11:20
 * @github https://github.com/0x7eTeam
 */
import com.sec421.controller.ui.MainController;
import com.sec421.core.Constants;
import com.sec421.tools.Tools;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InteractionExecController {
    public static Process shellProcess;
    public static PrintWriter shellWriter;
    public static ExecutorService executorService;
    private String command;
    private String line;
    private TextArea textArea;
    private String output;
    private String auth;
    public void exec(TextArea textArea,String exec_type, String execIP, String execNAME, String execPASS, String execAUTH, String execCHARSET) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.textArea = textArea;

        String pyPath = null;
        try {
            pyPath = Paths.get(Tools.getProperty("py_path")).toAbsolutePath().toString();
        } catch (IOException e) {
        }
        if (pyPath==null||pyPath.equals("")){
            Tools.alert("错误","请配置python.exe的路径");
        }
        Proxy proxy = (Proxy) MainController.settingInfo.get("proxy");
        if (proxy != null) {
            InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
            String proxyHost = proxyAddress.getHostString();
            int proxyPort = proxyAddress.getPort();
            if (proxy.type() == Proxy.Type.HTTP) {
                Tools.alert("错误","请使用socks代理");
            }else if (proxy.type() == Proxy.Type.SOCKS){
                System.setProperty("socksProxyHost", proxyHost);
                System.setProperty("socksProxyPort", Integer.toString(proxyPort));
            }
        }
        String script_name = Paths.get(Constants.IMPACKET_PATH+exec_type+".py").toAbsolutePath().toString();

        if (execAUTH=="密码"){
            this.auth = execNAME + ":" + execPASS;
        }else{
            this.auth = execNAME;
        }


        <fim_suffix>

        System.out.println(this.command);
            // 执行系统命令
        try {
            this.shellProcess = Runtime.getRuntime().exec(this.command);
            this.shellWriter = new PrintWriter(this.shellProcess.getOutputStream());

            this.executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.shellProcess.getInputStream(),execCHARSET))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String finalLine = line;
                        Platform.runLater(() -> this.textArea.appendText(finalLine + "\n"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendCommand(String command) {
        this.command = command;
        if (this.shellWriter != null) {
            this.shellWriter.println(this.command+ "\r\n");
            this.shellWriter.flush();
        }
    }
    public void stopSendCommand() {
        if (this.shellProcess != null) {
            this.shellWriter.println("exit\r\n");
            this.shellProcess.destroy();
            this.shellProcess = null;
            this.shellWriter = null;
            Platform.runLater(() -> this.textArea.appendText("已终止"));
        }
    }
}
<fim_middle>