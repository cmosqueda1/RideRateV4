package com.chris.uberratebanner;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.os.*;import android.provider.Settings;import android.text.TextUtils;import android.view.*;import android.widget.*;
public class MainActivity extends Activity{
 private static final int REQ_NOTIF=3002;private TextView state,button;private boolean pendingAccessibility=false;
 @Override protected void onCreate(Bundle b){super.onCreate(b);UiKit.applySystemBars(this);Notifications.ensureChannel(this);cleanupLegacyNotifications();requestNotif();build();}
 private void build(){LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(UiKit.BG);UiKit.applySafeInsets(shell,0,0,0,0);ScrollView sv=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(UiKit.dp(this,18),UiKit.dp(this,12),UiKit.dp(this,18),UiKit.dp(this,18));sv.addView(root);shell.addView(sv,new LinearLayout.LayoutParams(-1,0,1));shell.addView(UiKit.bottomNav(this,0));setContentView(shell);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_launcher);header.addView(logo,new LinearLayout.LayoutParams(UiKit.dp(this,42),UiKit.dp(this,42)));TextView title=UiKit.text(this,"RideRate",25,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=UiKit.dp(this,9);header.addView(title,tp);TextView gear=UiKit.text(this,"⚙",21,false);gear.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(gear);root.addView(header);root.addView(UiKit.muted(this,"Tap-to-analyze • offline OCR • low battery usage",13));
  state=UiKit.text(this,"RideRate\nStopped",19,true);state.setGravity(Gravity.CENTER);state.setBackground(UiKit.outlined(UiKit.BG,UiKit.DIVIDER,4,96,this));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(UiKit.dp(this,172),UiKit.dp(this,172));sp.gravity=Gravity.CENTER_HORIZONTAL;sp.topMargin=UiKit.dp(this,24);root.addView(state,sp);
  button=UiKit.primaryButton(this,"Start RideRate",v->toggle());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=UiKit.dp(this,18);root.addView(button,bp);
  TextView hint=UiKit.muted(this,"When active, a movable RideRate button floats over Uber. Tap it only when an offer is visible. RideRate takes one on-demand screenshot, OCRs only the bottom half, calculates the rate, and saves the result to History. It does not continuously record your screen.",12);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=UiKit.dp(this,12);root.addView(hint,hp);
  LinearLayout acts=new LinearLayout(this);acts.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=UiKit.dp(this,18);root.addView(acts,ap);acts.addView(UiKit.actionCard(this,"▣","Scan Image","from Gallery",v->startActivity(new Intent(this,ScanActivity.class))),new LinearLayout.LayoutParams(0,UiKit.dp(this,118),1));LinearLayout.LayoutParams hpp=new LinearLayout.LayoutParams(0,UiKit.dp(this,118),1);hpp.leftMargin=UiKit.dp(this,10);acts.addView(UiKit.actionCard(this,"▤","View History","offers & KPIs",v->startActivity(new Intent(this,HistoryActivity.class))),hpp);
 }
 private void toggle(){
  if(UberAccessibilityService.isRunning()){UberAccessibilityService.setRideRateActive(this,false);refresh();return;}
  if(!isAccessibilityEnabled()){pendingAccessibility=true;Toast.makeText(this,"Enable RideRate Live Detection, then return to RideRate.",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));return;}
  UberAccessibilityService.setRideRateActive(this,true);new Handler().postDelayed(this::refresh,180);
 }
 @Override protected void onResume(){super.onResume();if(pendingAccessibility&&isAccessibilityEnabled()){pendingAccessibility=false;UberAccessibilityService.setRideRateActive(this,true);}refresh();}
 private boolean isAccessibilityEnabled(){
  if(UberAccessibilityService.isConnected())return true;
  String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);if(TextUtils.isEmpty(enabled))return false;
  ComponentName cn=new ComponentName(this,UberAccessibilityService.class);String full=cn.flattenToString();String shortName=cn.flattenToShortString();
  for(String s:enabled.split(":"))if(s.equalsIgnoreCase(full)||s.equalsIgnoreCase(shortName))return true;return false;
 }
 private void refresh(){if(state==null)return;boolean on=UberAccessibilityService.isRunning();state.setText(on?"✓\nRideRate\nReady":"○\nRideRate\nStopped");state.setBackground(UiKit.outlined(UiKit.BG,on?UiKit.GREEN:UiKit.DIVIDER,4,96,this));button.setText(on?"Stop RideRate":"Start RideRate");}
 private void requestNotif(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);}
 private void cleanupLegacyNotifications(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=getSystemService(NotificationManager.class);try{nm.cancel(42);}catch(Exception ignored){}try{nm.deleteNotificationChannel("riderate_active");}catch(Exception ignored){}}}
}
