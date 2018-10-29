package de.appplant.cordova.plugin.badge;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.ruhuapp.kk.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import me.leolin.shortcutbadger.ShortcutBadger;

public class BadgeNumberManager {

  private Context mContext;

  private BadgeNumberManager(Context context) {
      mContext = context;
  }

  public static BadgeNumberManager from(Context context) {
      return new BadgeNumberManager(context);
  }

  private static final BadgeNumberManager.Impl IMPL;

  /**
   * 设置应用在桌面上显示的角标数字
   * @param number 显示的数字
   */
  public void setBadgeNumber(int number) {
      IMPL.setBadgeNumber(mContext, number);
  }
  public void clearBadge() { IMPL.clearBadge(mContext); }

  interface Impl {

      void setBadgeNumber(Context context, int number);
      void clearBadge(Context context);
  }

  static abstract class BadgeClear implements Impl {
      @Override
      public abstract void setBadgeNumber(Context context, int number);

      public void clearBadge (Context context) {
          setBadgeNumber(context, 0);
      }
  }

  static class ImplHuaWei extends BadgeClear {

      @Override
      public void setBadgeNumber(Context context, int number) {
          BadgeNumberManagerHuaWei.setBadgeNumber(context, number);
      }
  }

  static class ImplVIVO extends BadgeClear {

      @Override
      public void setBadgeNumber(Context context, int number) {
          BadgeNumberManagerVIVO.setBadgeNumber(context, number);
      }
  }

    static class ImplOPPO extends BadgeClear {

        @Override
        public void setBadgeNumber(Context context, int number) {
            BadgeNumberManagerOPPO.setBadgeNumber(context, number);
        }
    }

    static class ImplXIAOMI extends BadgeClear {

        @Override
        public void setBadgeNumber(Context context, int number) {
            BadgeNumberManagerXIAOMI.setBadgeNumber(context, number);
        }
    }


  static class ImplBase implements Impl {

      @Override
      public void setBadgeNumber(Context context, int number) {
          BadgeNumberManagerBASE.setBadgeNumber(context, number);
      }

      public void clearBadge (Context context) {
          BadgeNumberManagerBASE.clearBadge(context);
      }
  }

  static {
      String manufacturer = Build.MANUFACTURER.toLowerCase();
      if (manufacturer.contains("huawei")) {
          IMPL = new ImplHuaWei();
      } else if (manufacturer.contains("vivo")) {
          IMPL = new ImplVIVO();
      } else if (manufacturer.contains("oppo")) {
          IMPL = new ImplOPPO();
      } else if (manufacturer.contains("xiaomi")) {
          IMPL = new ImplXIAOMI();
      } else {
          IMPL = new ImplBase();
      }
  }
}

class BadgeNumberManagerHuaWei {
  public static void setBadgeNumber(Context context, int number) {
    try {
        if (number < 0) number = 0;
        Bundle bundle = new Bundle();
        bundle.putString("package", context.getPackageName());
        String launchClassName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();
        bundle.putString("class", launchClassName);
        bundle.putInt("badgenumber", number);
        context.getContentResolver().call(Uri.parse("content://com.huawei.android.launcher.settings/badge/"), "change_badge", null, bundle);
    } catch (Exception e) {
        e.printStackTrace();
    }
  }
}

// not work
class BadgeNumberManagerVIVO {
  public static void setBadgeNumber(Context context, int number) {
    try {
        Intent intent = new Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM");
        intent.putExtra("packageName", context.getPackageName());
        String launchClassName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();
        intent.putExtra("className", launchClassName);
        intent.putExtra("notificationNum", number);
        context.sendBroadcast(intent);
    } catch (Exception e) {
        e.printStackTrace();
    }
  }
}

// not work
class BadgeNumberManagerOPPO {
  public static void setBadgeNumber(Context context, int number) {
    try {
        if (number == 0) {
            number = -1;
        }
        Intent intent = new Intent("com.oppo.unsettledevent");
        intent.putExtra("pakeageName", context.getPackageName());
        intent.putExtra("number", number);
        intent.putExtra("upgradeNumber", number);
        if (canResolveBroadcast(context, intent)) {
            context.sendBroadcast(intent);
        } else {
            try {
                Bundle extras = new Bundle();
                extras.putInt("app_badge_count", number);
                context.getContentResolver().call(Uri.parse("content://com.android.badge/badge"), "setAppBadgeCount", null, extras);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  public static boolean canResolveBroadcast(Context context, Intent intent) {
      PackageManager packageManager = context.getPackageManager();
      List<ResolveInfo> receivers = packageManager.queryBroadcastReceivers(intent, 0);
      return receivers != null && receivers.size() > 0;
  }
}

class BadgeNumberManagerXIAOMI {
  //在调用NotificationManager.notify(notifyID, notification)这个方法之前先设置角标显示的数目

    private static BadgeNumberManagerXIAOMI mBadgeNumberManagerXIAOMI;
    private NotificationManager mNotificationManager;
    private String NOTIFICATION_CHANNEL_ID;
    private Notification mNotification;
    private String appName;
    private int mNotificationId = 0;
    public Context mContext;

    public BadgeNumberManagerXIAOMI (Context context) {
        mContext = context;
        appName = context.getResources().getString(R.string.app_name);

        mNotificationManager = (NotificationManager)context
                .getSystemService(Context.NOTIFICATION_SERVICE);

    }

    private void setBadgeNumberXIAOMI(int number) {
        mNotificationManager.cancel(mNotificationId);
        // mNotificationId ++;
        Notification.Builder builder = new Notification.Builder(mContext)
                .setContentTitle(appName).setContentText("有新消息").setSmallIcon(R.drawable.icon).setAutoCancel(true);

        NOTIFICATION_CHANNEL_ID = mContext.getPackageName() + "_notification_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel();

            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        mNotification = builder.build();
        try {
            Field field = mNotification.getClass().getDeclaredField("extraNotification");
            Object extraNotification = field.get(mNotification);
            Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
            method.invoke(extraNotification, number);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mNotificationManager.notify(mNotificationId, mNotification);
    }

  public static void setBadgeNumber(Context context, int number) {
      if (mBadgeNumberManagerXIAOMI == null) {
          mBadgeNumberManagerXIAOMI = new BadgeNumberManagerXIAOMI(context);
      }
        mBadgeNumberManagerXIAOMI.setBadgeNumberXIAOMI(number);
        /*
      String appName = context.getResources().getString(R.string.app_name);

      NotificationManager notificationManager = (NotificationManager)context
              .getSystemService(Context.NOTIFICATION_SERVICE);
      Notification.Builder builder = new Notification.Builder(context)
              .setContentTitle(appName).setContentText("有新消息").setSmallIcon(R.drawable.icon);

      Notification notification = builder.build();

      try {
          Field field = notification.getClass().getDeclaredField("extraNotification");
          Object extraNotification = field.get(notification);
          Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
          method.invoke(extraNotification, number);
      } catch (Exception e) {
          e.printStackTrace();
      }
      notificationManager.notify(0, notification);
      */
  }
    @TargetApi(Build.VERSION_CODES.O)
    private void setupNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, appName,
                NotificationManager.IMPORTANCE_DEFAULT);
        mNotificationManager.createNotificationChannel(channel);
    }
}

class BadgeNumberManagerBASE {
  public static void setBadgeNumber(Context context, int number) {
    ShortcutBadger.applyCount(context, number);
  }

  public static void clearBadge (Context context) {
      ShortcutBadger.removeCount(context);
  }
}
