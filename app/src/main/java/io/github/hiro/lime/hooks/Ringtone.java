package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

        public class Ringtone implements IHook {
            private android.media.Ringtone ringtone = null;
            private boolean isPlaying = false;

            @Override
            public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

                // Check if the call tone option is enabled
                if (!limeOptions.calltone.checked) return;

                // Hook into the specified method to handle received calls
                XposedBridge.hookAllMethods(
                        loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                        Constants.RESPONSE_HOOK.methodName,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                String paramValue = param.args[1].toString();
                                Context context = AndroidAppHelper.currentApplication().getApplicationContext();

                                // Check if the call has been received and the ringtone is not already playing
                                if (paramValue.contains("type:NOTIFIED_RECEIVED_CALL,") && !isPlaying) {
                                    if (context != null) {
                                        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                                        ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                        ringtone.play();
                                        isPlaying = true;
                                    }
                                }

                                // Handle the call rejection case and stop the ringtone
                                if (paramValue.contains("RESULT=REJECTED,") || paramValue.contains("RESULT=REJECTED,")) {
                                    if (ringtone != null && ringtone.isPlaying()) {
                                        ringtone.stop();
                                        isPlaying = false;
                                    }
                                }

                                // Handle the "NORMAL" result case for notification sound
                                if (paramValue.contains("contentType:CALL,") && paramValue.contains("RESULT=NORMAL")) {
                                    if (context != null) {
                                        Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                        android.media.Ringtone notificationRingtone = RingtoneManager.getRingtone(context, notificationUri);
                                        notificationRingtone.play();
                                    }
                                }
                            }
                        });

                // Hook the onCreate method of VoIPBaseFragment to stop the ringtone when needed
                Class<?> voIPBaseFragmentClass = loadPackageParam.classLoader.loadClass("com.linecorp.voip2.common.base.VoIPBaseFragment");
                XposedBridge.hookAllMethods(voIPBaseFragmentClass, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (ringtone != null && ringtone.isPlaying()) {
                            ringtone.stop();
                            isPlaying = false;
                        }
                    }
                });
            }

    }