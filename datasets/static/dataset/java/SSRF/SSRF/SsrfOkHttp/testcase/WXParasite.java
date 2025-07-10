<filename>FModule-main/Trunk/app/src/main/java/com/example/fmodule/hooks/WXParasite.java<fim_prefix>

        package com.example.fmodule.hooks;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.example.fmodule.hooks.models.RecvModel;
import com.example.fmodule.hooks.models.SendItemModel;
import com.example.fmodule.hooks.models.SendModel;
import com.example.fmodule.hooks.models.TalkModel;
import com.example.fmodule.hooktask.DiceTask;
import com.example.fmodule.hooktask.ReplyTask;
import com.example.fmodule.hooktask.SendTask;
import com.example.fmodule.hooktask.VoiceLenTask;
import com.example.fmodule.message.MessageManifest;
import com.example.fmodule.message.contacts.PContacts;
import com.example.fmodule.message.contacts.QContacts;
import com.example.fmodule.message.hooktask.MDiceTask;
import com.example.fmodule.message.hooktask.MReplyTask;
import com.example.fmodule.message.hooktask.MSendTask;
import com.example.fmodule.message.hooktask.MVoiceLenTask;
import com.example.fmodule.message.identify.PIdentify;
import com.example.fmodule.message.identify.QIdentify;
import com.example.fmodule.message.useravatar.PUserAvatar;
import com.example.fmodule.message.useravatar.QUserAvatar;
import com.example.fmodule.other.ContactData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import easynet.network.MessageHandler;
import easynet.network.Network;
import easynet.network.Session;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class WXParasite implements IXposedHookLoadPackage {
    private Handler mHandler;
    private XC_LoadPackage.LoadPackageParam lParam;
    private Context wxContext;
    private String wxId;
    private String nickname;
    private String avatarBase64;

    private UserInfoGetter userInfoGetter;
    private AvatarGetter avatarGetter;
    private MessageSender messageSender;
    private ContactsGetter contactsGetter;
    private EmojiSender emojiSender;

    private DiceTask diceTask;
    private VoiceLenTask voiceLenTask;
    private HashMap<String, HashMap<Integer, ReplyTask>> replyTasks = new HashMap<>();
    private Network network;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.tencent.mm")) {
            return;
        }
        mHandler = new Handler();
        lParam = loadPackageParam;

        //启动网络模块
        network = new Network();
        network.setMessageMgr(new MessageManifest());
        network.start();
        network.getMessageDispatcher().registerHandlers(this);

        hookContext();
        hookUserInfo();
        hookDice();
        hookReceiveMsg();
        hookVoice();
    }
    //hook当前上下文
    private void hookContext() {
        Class<?> clazz = XposedHelpers.findClass("android.content.ContextWrapper", lParam.classLoader);

        XposedHelpers.findAndHookMethod(
                clazz,
                "getApplicationContext",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (wxContext != null) {
                            return;
                        }
                        wxContext = (Context) param.getResult();
                    }
                }
        );
//        HookEntryUtil.encryptStrCallback = new HookEntryUtil.EncryptStrCallback() {
//            @Override
//            public String run(String str) {
//                Random r = new Random();
//                char a = (char)(r.nextInt(26) + 65);
//                char b = (char)(r.nextInt(26) + 65);
//                return String.valueOf(a).concat(str).concat(String.valueOf(b));
//            }
//        };
        HookEntryUtil.decryptStrCallback = new DecryptStrCallback(1, 1);
    }
    //hook用户信息
    private void hookUserInfo() {
        userInfoGetter = new UserInfoGetter(lParam);
        contactsGetter = new ContactsGetter(lParam);
        avatarGetter = new AvatarGetter(lParam);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                int times = 5;
                while (true) {
                    try {
                        if (i > times) {
                            return;
                        }
                        Thread.sleep(3000);
                        //XposedBridge.log("第" + (i++) + "次尝试：ThreadId=" + Thread.currentThread().getId());
                        Cursor cursor = contactsGetter.get();
                        if (cursor == null || cursor.getCount() == 0) {
                            continue;
                        }
                        //XposedBridge.log("拿到通讯录");
                        //拿微信号
                        wxId = userInfoGetter.getWxId();
                        //XposedBridge.log("拿到微信号");
                        //拿微信昵称
                        nickname = userInfoGetter.getNickname();
                        //拿微信头像
                        avatarBase64 = avatarGetter.getBase64(wxId);
                        if (avatarBase64 == null) {
                            continue;
                        }
                        onInitialized();
                        return;
                    }catch (Exception e) {
                        //XposedBridge.log("第" + i + "次尝试失败！" + e);
                    }
                }
            }
        }).start();
    }
    //hook骰子
    private void hookDice() {
        new DiceHooker(lParam, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (diceTask == null || !diceTask.isOn) {
                    return;
                }
                if (diceTask.number < 1 || diceTask.number > 6) {
                    return;
                }
                param.setResult(diceTask.number - 1);
            }
        });
    }
    //hook接收消息
    private void hookReceiveMsg() {
        new ReceiveMsgHooker(lParam, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                //Toast.makeText(wxContext, "劫持到消息", Toast.LENGTH_SHORT).show();
                //XposedBridge.log("劫持成功： args0: " + param.args[0] + "   args1: " + param.args[1]);
                String tableName = (String) param.args[0];
                if (tableName.isEmpty() || !tableName.equals("message")) {
                    return;
                }
                ContentValues contentValues = (ContentValues) param.args[2];
                Integer type = contentValues.getAsInteger("type");
                if (type == null || !WXMessageUtil.containsType(type)) {
                    return;
                }

                String content = (String)contentValues.get("content");
                String talker = (String)contentValues.get("talker");

                //处理联系人的回复任务
                HashMap<Integer, ReplyTask> tasks = replyTasks.get(talker);
                if (tasks != null) {
                    for (ReplyTask task : tasks.values()) {
                        handleReplyTask(task, talker, content, type, contentValues);
                    }
                }
                //处理全局的回复任务
                tasks = replyTasks.get("all");
                if (tasks != null) {
                    for (ReplyTask task : tasks.values()) {
                        handleReplyTask(task, talker, content, type, contentValues);
                    }
                }
            }
            //处理任务
            private void handleReplyTask(ReplyTask task, String talker, String content, int type, ContentValues values) {
                if (!task.isOn) {
                    return;
                }
                boolean isValidated = false;
                switch (task.patternMode) {
                    case precise:
                    case fuzzy:
                        if (type != 1) {
                            return;
                        }
                        isValidated = task.patternMode == ReplyTask.PatternMode.precise ? content.equals(task.patternMsg) : content.contains(task.patternMsg);
                        break;
                    case all:
                        isValidated = true;
                        break;
                }

                if (!isValidated) {
                    return;
                }

                try {
                    switch (task.replyMode) {
                        case fixedText:
                            messageSender.send(talker, task.replyMsg);
                            break;
                        case emojiUrl:
                            emojiSender.send(talker, task.emojiUrl);
                            break;
                        case serverApi:
                            postContent(task.serverApi, talker, content, type, values);
                            break;
                    }
                }catch (Exception e) {
                    XposedBridge.log("handleReplyTask error: " + e);
                }
            }
            //提交到远程接口
            private void postContent(String url, String talker, String content, int type, ContentValues values) {
                if (url == null || url.trim().isEmpty()) {
                    return;
                }
                Request request = new Request.Builder()
                        .url(url)
                        .post(new RequestBody() {
                            @Nullable
                            @Override
                            public MediaType contentType() {
                                return MediaType.parse("application/json; charset=utf-8");
                            }
                            @Override
                            public void writeTo(BufferedSink sink) throws IOException {
                                RecvModel rm = new RecvModel();
                                rm.FromUser = talker;
                                rm.MsgId = values.getAsInteger("msgId");
                                rm.Content = content;
                                rm.Status = values.getAsInteger("status");
                                rm.CreateTime = values.getAsLong("createTime");
                                rm.IsSend = values.getAsInteger("isSend");
                                rm.Type = type;
                                sink.writeUtf8(JSON.toJSONString(rm));
                            }
                        }).build();
                OkHttpClient client = new OkHttpClient();
                client.newCall(request)
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                XposedBridge.log("okHttp请求错误：" + e.getMessage());
                            }
                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                int code = response.code();
                                String respContent = response.body().string();
                                XposedBridge.log("okHttp请求完成：" + code + " 响应内容：" + respContent);
                                if (code != 200) {
                                    return;
                                }
                                SendModel sm = JSON.parseObject(respContent, SendModel.class);
                                handleSendModel(sm);
                            }
                        });
            }
        });
    }
    //hook语音
    private void hookVoice() {
        new VoiceHooker(lParam, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (voiceLenTask == null || !voiceLenTask.isOn) {
                    return;
                }
                param.args[1] = voiceLenTask.number * 1000;
            }
        });
    }
    //微信初始化完成
    private void onInitialized() {
        messageSender = new MessageSender(lParam);
        emojiSender = new EmojiSender(lParam, wxContext);
        HookEntryUtil.encryptStrCallback = null;
        HookEntryUtil.decryptStrCallback = null;
        XposedBridge.log("初始化信息完成，开始连接FModule");
        connectServer();
    }
    //连接服务器，并认证身份
    private void connectServer() {
        Session session = network.createSessionAsync("127.0.0.1", 63333).execute();
        if (session == null) {
            //XposedBridge.log("连接FModule失败！请确保已启动FModule");
            if (wxContext != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(wxContext, "连接FModule失败！请确保已启动FModule", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return;
        }
        session.registerErrorCallback(new Session.ErrorCallback() {
            @Override
            public void run(Session session, Exception e) {
                if (wxContext != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(wxContext, "微信与FModule断开连接，请重启后再次尝试", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        XposedBridge.log("连接成功，开始请求验证身份");
        QIdentify qIdentify = new QIdentify();
        qIdentify.identity = QIdentify.wx;
        qIdentify.id = wxId;
        qIdentify.nickname = nickname;
        qIdentify.avatarBase64 = avatarBase64;
        PIdentify response = (PIdentify) session.call(qIdentify).execute();
        XposedBridge.log("身份验证结果： " + response.result);
        if (!response.result) {
            session.dispose();
        }
    }
    //骰子hook任务改变
    @MessageHandler
    public void mDiceTaskHandler(Session session, MDiceTask mDiceTask) {
        diceTask = mDiceTask.diceTask;
    }
    //自动回复任务改变
    @MessageHandler
    public void mReplyTaskHandler(Session session, MReplyTask mReplyTask) {
        HashMap<Integer, ReplyTask> tasks = replyTasks.get(mReplyTask.replyTask.fromUser);
        if (tasks == null) {
            tasks = new HashMap<>();
            replyTasks.put(mReplyTask.replyTask.fromUser, tasks);
        }
        if (mReplyTask.opType == MReplyTask.OpType.delete) {
            tasks.remove(mReplyTask.replyTask.id);
            return;
        }
        tasks.put(mReplyTask.replyTask.id, mReplyTask.replyTask);
    }
    //发送消息
    @MessageHandler
    public void mSendTaskHandler(Session session, MSendTask mSendTask) {
        try {
            SendTask task = mSendTask.sendTask;
            switch (task.sendMode) {
                case fixedText:
                    if (task.toUsers == null || task.toUsers.isEmpty()) {
                        messageSender.send(task.toUser, task.sendMsg);
                    }else {
                        String[] toUsers = task.toUsers.split(":");
                        for (String s : toUsers) {
                            messageSender.send(s, task.sendMsg);
                        }
                    }
                    break;
                case emojiUrl:
                    if (task.toUsers == null || task.toUsers.isEmpty()) {
                        emojiSender.send(task.toUser, task.emojiUrl);
                    }else {
                        String[] toUsers = task.toUsers.split(":");
                        for (String s : toUsers) {
                            emojiSender.send(s, task.emojiUrl);
                        }
                    }
                    break;
                case serverApi:
                    if (task.toUsers == null || task.toUsers.isEmpty()) {
                        postContent(task.serverApi, task.toUser);
                    }else {
                        String[] toUsers = task.toUsers.split(":");
                        for (String s : toUsers) {
                            postContent(task.serverApi, s);
                        }
                    }
                    break;
            }
        }catch (Exception e) {

        }
    }
    //提交到远程接口
    private void postContent(String url, String talker) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        <fim_suffix>
        Request request = new Request.Builder()
                .url(url)
                .post(new RequestBody() {
                    @Nullable
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse("application/json; charset=utf-8");
                    }
                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        TalkModel sm = new TalkModel();
                        sm.Talker = talker;
                        String str = JSON.toJSONString(sm);
                        sink.writeUtf8(str);
                    }
                }).build();
        OkHttpClient client = new OkHttpClient();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        XposedBridge.log("okHttp请求错误：" + e.getMessage());
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        int code = response.code();
                        String respContent = response.body().string();
                        XposedBridge.log("okHttp请求完成：" + code + " 响应内容：" + respContent);
                        if (code != 200) {
                            return;
                        }
                        SendModel sm = JSON.parseObject(respContent, SendModel.class);
                        handleSendModel(sm);
                    }
                });
    }
    //处理响应的发送内容
    private void handleSendModel(SendModel sm) {
        if (!sm.IsSend || sm.ToUser == null || sm.ToUser.isEmpty()) {
            return;
        }
        for (SendItemModel item : sm.SendList) {
            if (item.Delay <= 0) {
                handleSendItemModel(sm.ToUser, item);
                continue;
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleSendItemModel(sm.ToUser, item);
                }
            }, item.Delay);
        }
    }
    //处理响应的发送内容条目
    private void handleSendItemModel(String toUser, SendItemModel item) {
        try {
            switch (item.Type) {
                case 0:
                    messageSender.send(toUser, item.Content);
                    break;
                case 1:
                    emojiSender.send(toUser, item.Content);
                    break;
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
    //获取通讯录
    @MessageHandler
    public void qContactsHandler(Session session, QContacts qContacts) {
        PContacts response = new PContacts();
        response.rpcId = qContacts.rpcId;
        response.contacts = new ArrayList<>();
        try {
            Cursor cursor = contactsGetter.get();
            int count = cursor.getCount();
            for(int i = 0; i<count; i++) {
                cursor.moveToPosition(i);
                ContactData data = new ContactData();
                data.convertFrom(cursor);
                response.contacts.add(data);
            }
        }catch (Exception e) {
            XposedBridge.log("获取通讯录失败！" + e);
        }
        session.send(response);
    }
    //获取用户头像
    @MessageHandler
    public void qUserAvatar(Session session, QUserAvatar qUserAvatar) {
        PUserAvatar pUserAvatar = new PUserAvatar();
        pUserAvatar.rpcId = qUserAvatar.rpcId;
        pUserAvatar.wxId = qUserAvatar.wxId;
        try {
            pUserAvatar.avatarData = avatarGetter.getBytes(qUserAvatar.wxId);
        }catch (Exception e) {
            //XposedBridge.log("get Avatar error: " + e);
        }
        session.send(pUserAvatar);
        //XposedBridge.log("QUserAvatar:  wxId=" + pUserAvatar.wxId + "  pUserAvatar.avatarData == null: " + (pUserAvatar.avatarData == null));
    }
    //控制语音秒数任务改变
    @MessageHandler
    public void mVoiceLenTaskHandler(Session session, MVoiceLenTask data) {
        voiceLenTask = data.task;
    }
}
<fim_middle>