package com.hyphenate.easeui.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMMessage.ChatType;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.domain.EaseEmojicon;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseAtMessageHelper;
import com.hyphenate.easeui.model.EaseNotifier;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public final class EaseUI {
    /**
     * 类名
     */
    private static final String TAG = EaseUI.class.getSimpleName();

    /**
     * easeUI实例
     * the global EaseUI instance
     */
    private static EaseUI instance = null;
    
    /**
     * 用户信息提供者
     * user profile provider
     *
     */
    private EaseUserProfileProvider userProvider;
    
    private EaseSettingsProvider settingsProvider;
    
    /**
     * application 上下文
     * application context
     */
    private Context appContext = null;
    
    /**
     * false sdk没有初始化
     * init flag: test if the sdk has been inited before, we don't need to init again
     */
    private boolean sdkInited = false;
    
    /**
     * 通知者
     * the notifier
     */
    private EaseNotifier notifier = null;
    
    /**
     * 注册了eventlistener 的aty的集合
     * save foreground Activity which registered eventlistener
     */
    private List<Activity> activityList = new ArrayList<Activity>();

    /**
     * 向下面集合添加aty
     * 注册了eventlistener 的aty的集合
     * @param activity
     */
    public void pushActivity(Activity activity){
        if(!activityList.contains(activity)){
            activityList.add(0,activity); 
        }
    }

    /**
     * 向下面集合移除aty
     * 注册了eventlistener 的aty的集合
     * @param activity
     */
    public void popActivity(Activity activity){
        activityList.remove(activity);
    }
    
    
    private EaseUI(){}
    
    /**
     * get EaseUI实例
     * get instance of EaseUI
     * @return
     */
    public synchronized static EaseUI getInstance(){
        if(instance == null){
            instance = new EaseUI();
        }
        return instance;
    }
    
    /**
     * 初始化sdk及easeUI kit
     *this function will initialize the SDK and easeUI kit
     * 
     * @return boolean true if caller can continue to call SDK related APIs after calling onInit, otherwise false.
     * 
     * @param context
     * @param options use default if options is null
     * @return
     */
    public synchronized boolean init(Context context, EMOptions options){

        if(sdkInited){
            //初始化过了不再初始化
            return true;
        }
        appContext = context;
        
        int pid = android.os.Process.myPid();//当前应用进程
        String processAppName = getAppName(pid);
        
        Log.d(TAG, "process app name : " + processAppName);

        // if there is application has remote service, application:onCreate() maybe called twice
        // this check is to make sure SDK will initialized only once
        // return if process name is not application's name since the package name is the default process name
        if (processAppName == null || !processAppName.equalsIgnoreCase(appContext.getPackageName())) {
            Log.e(TAG, "enter the service process!");
            return false;
        }
        //初始化EMClient
        if(options == null){
            EMClient.getInstance().init(context, initChatOptions());
        }else{
            EMClient.getInstance().init(context, options);
        }
        
        initNotifier();
        registerMessageListener();
        
        if(settingsProvider == null){
            settingsProvider = new DefaultSettingsProvider();
        }
        
        sdkInited = true;
        return true;
    }
    

    protected EMOptions initChatOptions(){
        Log.d(TAG, "init HuanXin Options");

        EMOptions options = new EMOptions();
        // change to need confirm contact invitation
        options.setAcceptInvitationAlways(false);
        // set if need read ack
        options.setRequireAck(true);
        // set if need delivery ack
        options.setRequireDeliveryAck(false);
        
        return options;
    }

    /**
     * 初始化通知这
     */
    void initNotifier(){
        notifier = createNotifier();
        notifier.init(appContext);
    }
    
    private void registerMessageListener() {
        EMClient.getInstance().chatManager().addMessageListener(new EMMessageListener() {
            
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                EaseAtMessageHelper.get().parseMessages(messages);
            }
            @Override
            public void onMessageReadAckReceived(List<EMMessage> messages) {
                
            }
            @Override
            public void onMessageDeliveryAckReceived(List<EMMessage> messages) {
            }
            @Override
            public void onMessageChanged(EMMessage message, Object change) {
                
            }
            @Override
            public void onCmdMessageReceived(List<EMMessage> messages) {
                
            }
        });
    }
    
    protected EaseNotifier createNotifier(){
        return new EaseNotifier();
    }
    
    public EaseNotifier getNotifier(){
        return notifier;
    }
    
    public boolean hasForegroundActivies(){
        return activityList.size() != 0;
    }
    
    /**
     * set user profile provider
     * @param provider
     */
    public void setUserProfileProvider(EaseUserProfileProvider userProvider){
        this.userProvider = userProvider;
    }
    
    /**
     * get user profile provider
     * @return
     */
    public EaseUserProfileProvider getUserProfileProvider(){
        return userProvider;
    }
    
    public void setSettingsProvider(EaseSettingsProvider settingsProvider){
        this.settingsProvider = settingsProvider;
    }
    
    public EaseSettingsProvider getSettingsProvider(){
        return settingsProvider;
    }
    
    
    /**
     * check the application process name if process name is not qualified, then we think it is a service process and we will not init SDK
     * @param pID
     * @return
     */
    private String getAppName(int pID) {
        String processName = null;
        ActivityManager am = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        PackageManager pm = appContext.getPackageManager();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pID) {
                    CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(info.processName, PackageManager.GET_META_DATA));
                    // Log.d("Process", "Id: "+ info.pid +" ProcessName: "+
                    // info.processName +"  Label: "+c.toString());
                    // processName = c.toString();
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                // Log.d("Process", "Error>> :"+ e.toString());
            }
        }
        return processName;
    }
    
    /**
     * User profile provider
     * @author wei
     *
     */
    public interface EaseUserProfileProvider {
        /**
         * return EaseUser for input username
         * @param username
         * @return
         */
        EaseUser getUser(String username);
    }
    
    /**
     * Emojicon provider
     *
     */
    public interface EaseEmojiconInfoProvider {
        /**
         * return EaseEmojicon for input emojiconIdentityCode
         * @param emojiconIdentityCode
         * @return
         */
        EaseEmojicon getEmojiconInfo(String emojiconIdentityCode);
        
        /**
         * get Emojicon map, key is the text of emoji, value is the resource id or local path of emoji icon(can't be URL on internet)
         * @return
         */
        Map<String, Object> getTextEmojiconMapping();
    }
    
    private EaseEmojiconInfoProvider emojiconInfoProvider;
    
    /**
     * Emojicon provider
     * @return
     */
    public EaseEmojiconInfoProvider getEmojiconInfoProvider(){
        return emojiconInfoProvider;
    }
    
    /**
     * set Emojicon provider
     * @param emojiconInfoProvider
     */
    public void setEmojiconInfoProvider(EaseEmojiconInfoProvider emojiconInfoProvider){
        this.emojiconInfoProvider = emojiconInfoProvider;
    }
    
    /**
     * new message options provider
     *
     */
    public interface EaseSettingsProvider {
        boolean isMsgNotifyAllowed(EMMessage message);
        boolean isMsgSoundAllowed(EMMessage message);
        boolean isMsgVibrateAllowed(EMMessage message);
        boolean isSpeakerOpened();
    }
    
    /**
     * default settings provider
     *
     */
    protected class DefaultSettingsProvider implements EaseSettingsProvider{

        @Override
        public boolean isMsgNotifyAllowed(EMMessage message) {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public boolean isMsgSoundAllowed(EMMessage message) {
            return true;
        }

        @Override
        public boolean isMsgVibrateAllowed(EMMessage message) {
            return true;
        }

        @Override
        public boolean isSpeakerOpened() {
            return true;
        } 
    }
    
    public Context getContext(){
        return appContext;
    }
}
