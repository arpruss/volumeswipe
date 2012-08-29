package mobi.omegacentauri.VolumeSwipe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DecimalFormat;

import mobi.omegacentauri.VolumeSwipe.R;
import mobi.omegacentauri.VolumeSwipe.VolumeSwipeService.IncomingHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class VolumeSwipe extends Activity implements ServiceConnection {
	public static final boolean DEBUG = true;
	static final String MARKET = "Appstore";
	
	public static final DecimalFormat decimal = new DecimalFormat("0.0");
	

	private CheckBox activeBox;
	private CheckBox boostBox;
	private boolean active;

	private Messenger messenger = null;

	private SharedPreferences options;
	private NotificationManager notificationManager;
	private boolean getOut = false;

	static final int NOTIFICATION_ID = 1;

	public static void log(String s) {
		if (DEBUG)
			Log.v("VolumeSwipe", s);
	}
	
	public void onNarrow(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_NARROW).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onVNarrow(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_VNARROW).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onMedium(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_MEDIUM).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onWide(View v) {
		options.edit().putInt(Options.PREF_WIDTH, Options.OPT_WIDE).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onLeft(View v) {
		options.edit().putBoolean(Options.PREF_LEFT, true).commit();
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void onRight(View v) {
		options.edit().putBoolean(Options.PREF_LEFT, false).commit();		
		sendMessage(IncomingHandler.MSG_ADJUST, 0, 0);
	}
	
	public void contextMenuOnClick(View v) {
		v.showContextMenu();
	}
	
	public void adOnClick(View v) {
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if (MARKET.contains("arket"))
    		i.setData(Uri.parse("market://search?q=pub:\"Omega Centauri Software\""));
    	else
    		i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=mobi.omegacentauri.ScreenDim.Full&showAll=1"));            		
    	startActivity(i);
	}
	
	private void message(String title, String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(Html.fromHtml(msg));
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				getResources().getText(R.string.ok), 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {} });
		alertDialog.show();

	}
	
	private void firstTime() {
		if (! options.getBoolean(Options.PREF_FIRST_TIME, true))
			return;

		SharedPreferences.Editor ed = options.edit();
		ed.putBoolean(Options.PREF_FIRST_TIME, false);
		ed.commit();           
		
		String msg;
		msg = "With VolumeSwipe, you can set the volume in the Amazon video player and "+
			  "selected other applications by swiping along the side of the screen.  You "+
			  "are responsible for ensuring that the volume boost feature does not damage your hearing.";
		message("Welcome", msg);
	}

	public void menuButton(View v) {
		openOptionsMenu();
	}
	
	public void helpButton(View v) {
		help();
	}
	
	private void saveOS() {
	}
	
	private void restoreOS() {
	}
	
	void stopService() {
		log("stop service");
		stopService(new Intent(this, VolumeSwipeService.class));
		restoreOS();
	}
	
	void saveSettings() {
	}
	
	void bind() {
		log("bind");
		Intent i = new Intent(this, VolumeSwipeService.class);
		bindService(i, this, 0);
	}
	
	void restartService(boolean bind) {
		stopService();
		saveSettings();		
		Intent i = new Intent(this, VolumeSwipeService.class);
		startService(i);
		if (bind) {
			bind();
		}
	}
	
	void setActive(boolean value, boolean bind) {
		SharedPreferences.Editor ed = options.edit();
		ed.putBoolean(Options.PREF_ACTIVE, value);
		ed.commit();
		if (value) {
			restartService(bind);
		}
		else {
			stopService();
		}
		active = value;
		updateNotification();
	}
	
	public void PleaseBuy(String title, String text, final boolean exit) {		
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setTitle(title);        
        alertDialog.setMessage(Html.fromHtml(text));
        
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"Go to "+MARKET, 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	market();
            	if (exit) {
        			stopService();
            		finish();
            	}
            } });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		"Not now", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	if (exit) {
        			stopService();
            		finish();
            	}
            	
            } });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if (exit)
					finish();
			}
		});
        alertDialog.show();				
	}
	
	private void market() {
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	if (MARKET.contains("Appstore")) {
          // string split up to fool switcher.sh
    		i.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=mobi.omegacentauri.Screen" +"Dim."+"Full"));
    	}
    	else if (MARKET.contains("AndroidPIT")) {
            // string split up to fool switcher.sh
    		i.setData(Uri.parse("http://www.androidpit.com/en/android/market/apps/app/mobi.omegacentauri.Screen" +"Dim."+"Full"));
    	}
    	else {
          // string split up to fool switcher.sh
    		i.setData(Uri.parse("market://details?id=mobi.omegacentauri.Screen" +"Dim."+"Full"));
    	}
    	startActivity(i);
	}
	
	private void changeLog() {
		message("Changes", getAssetFile("changelog.html"));
	}
	
	private void help() {
		message("Questions and Answers", getAssetFile(isKindle()?"kindlehelp.html":"help.html"));
	}
	
	private void versionUpdate() {
		int versionCode;
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			versionCode = 0;
		} 
		if (options.getInt(Options.PREF_LAST_VERSION, 0) != versionCode) {
			options.edit().putInt(Options.PREF_LAST_VERSION, versionCode).commit();
			changeLog();
		}
			
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		options = PreferenceManager.getDefaultSharedPreferences(this);
		
		super.onCreate(savedInstanceState);

		versionUpdate();
		firstTime();
		
    	setContentView(R.layout.main);

    	notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		active = options.getBoolean(Options.PREF_ACTIVE, false);

		activeBox = (CheckBox)findViewById(R.id.active);
		activeBox.setChecked(active);
		activeBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean value) {
				if (value && !active) {
					saveOS();
				}
				setActive(value, true);
				
				if (value) 
					message("Important information",
							"If you wish to uninstall or upgrade VolumeSwipe, uncheck the 'Active' "+
							"button before uninstalling or upgrading, or some system resources "+
							"will be wasted (they can be reclaimed by rebooting your device).");
			}});
		
		boostBox = (CheckBox)findViewById(R.id.boost);
		boostBox.setChecked(options.getBoolean(Options.PREF_BOOST, false));
		boostBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton button, boolean value) {
				options.edit().putBoolean(Options.PREF_BOOST, value).commit();
				sendMessage(IncomingHandler.MSG_BOOST, 0, 0);
			}});		
		
		if (Build.VERSION.SDK_INT < 9)
			boostBox.setVisibility(View.GONE);
	}

	public static void setNotification(Context c, NotificationManager nm, boolean active) {
		Notification n = new Notification(
				active?R.drawable.brightnesson:R.drawable.brightnessoff,
				"VolumeSwipe", 
				System.currentTimeMillis());
		Intent i = new Intent(c, VolumeSwipe.class);		
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT; 
		n.setLatestEventInfo(c, "VolumeSwipe", "VolumeSwipe is "+(active?"on":"off"), 
				PendingIntent.getActivity(c, 0, i, 0));
		nm.notify(NOTIFICATION_ID, n);
		log("notify "+n.toString());
	}
	
	private void updateNotification() {
		updateNotification(this, options, notificationManager, active);
	}
	
	public static void updateNotification(Context c, 
			SharedPreferences options, NotificationManager nm, boolean active) {
		log("notify "+Options.getNotify(options));
		switch(Options.getNotify(options)) {
		case Options.NOTIFY_NEVER:
			nm.cancelAll();
			break;
		case Options.NOTIFY_AUTO:
			if (active)
				setNotification(c, nm, active);
			else {
				log("trying to cancel notification");
				nm.cancelAll();
			}
			break;
		case Options.NOTIFY_ALWAYS:
			setNotification(c, nm, active);
			break;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (active) {			
			restartService(true);
		}
		else {
			stopService();
		}
		
		((RadioButton)findViewById(R.id.left)).setChecked(
				options.getBoolean(Options.PREF_LEFT, true));
		((RadioButton)findViewById(R.id.right)).setChecked(
				!options.getBoolean(Options.PREF_LEFT, true));
		((RadioButton)findViewById(R.id.narrow)).setChecked(
				options.getInt(Options.PREF_WIDTH, Options.OPT_MEDIUM)==Options.OPT_NARROW);
		((RadioButton)findViewById(R.id.medium)).setChecked(
				options.getInt(Options.PREF_WIDTH, Options.OPT_MEDIUM)==Options.OPT_MEDIUM);
		((RadioButton)findViewById(R.id.wide)).setChecked(
				options.getInt(Options.PREF_WIDTH, Options.OPT_MEDIUM)==Options.OPT_WIDE);
		((RadioButton)findViewById(R.id.vnarrow)).setChecked(
				options.getInt(Options.PREF_WIDTH, Options.OPT_MEDIUM)==Options.OPT_VNARROW);
		((TextView)findViewById(R.id.info)).setText(
				options.getBoolean(Options.PREF_ACTIVE_IN_ALL, false) ? 
						R.string.infoActiveInAll : R.string.info);
		
		updateNotification();
		
		if (getOut) { 
			log("getting out");
			return;
		}
		
		if (active) {
			bind();
		}			
	}

	@Override
	public void onPause() {
		super.onPause();
		
		if (getOut)
			return;
		
		saveSettings();

		if (messenger != null) {
			log("unbind");
			unbindService(this);
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		log("onStop");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		log("onDestroy");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.change_log:
			changeLog();
			return true;
		case R.id.help:
			help();
			return true;
//		case R.id.please_buy:
//			new OtherApps(this, true);
//			return true;
		case R.id.options:
			startActivity(new Intent(this, Options.class));			
			return true;
		default:
			return false;
		}
	}
	
	private boolean largeScreen() {
		int layout = getResources().getConfiguration().screenLayout;
		return (layout & Configuration.SCREENLAYOUT_SIZE_MASK) == 
			        Configuration.SCREENLAYOUT_SIZE_LARGE;		
	}
	
	private boolean isKindle() {
		return Build.MODEL.equalsIgnoreCase("Kindle Fire");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}


	public void sendMessage(int n, int arg1, int arg2) {
		if (messenger == null) 
			return;
		
		try {
			log("message "+n+" "+arg1+" "+arg2);
			messenger.send(Message.obtain(null, n, arg1, arg2));
		} catch (RemoteException e) {
		}
	}
	
	@Override
	public void onServiceConnected(ComponentName classname, IBinder service) {
		log("connected");
		messenger = new Messenger(service);
		try {
			messenger.send(Message.obtain(null, IncomingHandler.MSG_ON, 0, 0));
			messenger.send(Message.obtain(null, IncomingHandler.MSG_VISIBLE, 0, 0));
		} catch (RemoteException e) {
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		log("disconnected"); 
//		stopService(new Intent(this, VolumeSwipeService.class));
		messenger = null;		
	}

	static private String getStreamFile(InputStream stream) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(stream));

			String text = "";
			String line;
			while (null != (line=reader.readLine()))
				text = text + line;
			return text;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
	
	public String getAssetFile(String assetName) {
		try {
			return getStreamFile(getAssets().open(assetName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
}

