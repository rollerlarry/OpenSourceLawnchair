package ch.deletescape.lawnchair;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;
import android.widget.Switch;

import org.jsoup.Jsoup;


import ch.deletescape.lawnchair.preferences.IPreferenceProvider;

public class UpdateAppFromStore extends Launcher.LauncherDialog{
    ComponentName component;
    private static IPreferenceProvider sharedPrefs;
    private EditableItemInfo info;
    private EditText title;
    private Switch visibility;
    private boolean visibleState;
    private Launcher launcher;

    public UpdateAppFromStore(@NonNull Context context, EditableItemInfo info, Launcher launcher) {
        super(context);
        this.info = info;
        this.launcher = launcher;
        sharedPrefs = Utilities.getPrefs(context.getApplicationContext());
        setCanceledOnTouchOutside(true);
    }

    public void launchPlayStore(Context context, String packageName) {
        Intent intent = null;
        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException anfe) {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.app_edit_dialog);

        component = info.getComponentName();
        String packageName = component.getPackageName();

        launchPlayStore(getContext(), packageName);
        checkAppVersionCurrent();
        new GetVersionCode().execute();

    }

    private String checkAppVersionCurrent() {
        String version = "";
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(component.getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            System.out.println(pInfo.sharedUserId+"aaaaaaaa");

            System.out.println(verCode+"123");
            System.out.println(version+"123");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    private class GetVersionCode extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... voids) {

            String newVersion = null;
            try {
                newVersion = Jsoup.connect("https://play.google.com/store/apps/details?id=" + getContext().getPackageName() + "&hl=it")
                        .timeout(30000)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get()
                        .select("div[itemprop=softwareVersion]")
                        .first()
                        .ownText();
                return newVersion;
            } catch (Exception e) {
                return newVersion;
            }
        }

        @Override
        protected void onPostExecute(String onlineVersion) {
            super.onPostExecute(onlineVersion);
            String currentVersion = checkAppVersionCurrent();

            if (onlineVersion != null && !onlineVersion.isEmpty()) {
                if (Float.valueOf(currentVersion) < Float.valueOf(onlineVersion)) {
                    System.out.println("new updateeeeeeeeeeeee");
                }
            }
            Log.d("update", "Current version " + currentVersion + "playstore version " + onlineVersion);
        }
    }

}
