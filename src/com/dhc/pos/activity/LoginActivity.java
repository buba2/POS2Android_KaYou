package com.dhc.pos.activity;

import java.util.HashMap;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dhc.pos.activity.view.PwdWithLabelView;
import com.dhc.pos.agent.client.AppDataCenter;
import com.dhc.pos.agent.client.ApplicationEnvironment;
import com.dhc.pos.agent.client.Constant;
import com.dhc.pos.dynamic.core.Event;
import com.dhc.pos.dynamic.core.ViewPage;
import com.dhc.pos.util.APKUtil;
import com.dhc.pos.util.LocationUtil;
import com.dhc.pos.R;

public class LoginActivity extends BaseActivity implements OnClickListener, TextWatcher{
	
	private EditText usernameET						= null;
	private PwdWithLabelView passwordET				= null;
	private EditText securiteCodeET					= null;
	private Button loginButton 						= null;
	private Button registerButton 					= null;
	private Button securityCodeButton   			= null;
	private TextView forgetPwdTV					= null;
	
	private boolean hasInit							= false;
	
	public static int getSecurityCodeCount			= 0;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        setContentView(R.layout.login);
        
        this.getIntent().setAction("com.dhc.pos.login");
        
        // 首先取得并设置当前版本号
        try{
        	PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            int versionCode = info.versionCode;
            AppDataCenter.setVersionCode(versionCode);
        } catch(Exception e){
        	e.printStackTrace();
        }
        
        usernameET = (EditText)findViewById(R.id.usernameET);
        passwordET = (PwdWithLabelView)findViewById(R.id.passwordET);
        securiteCodeET = (EditText)findViewById(R.id.securiteCodeET);
        securityCodeButton = (Button)this.findViewById(R.id.securityCodeButton);
        loginButton = (Button)findViewById(R.id.loginButton);
        registerButton = (Button)findViewById(R.id.registerButton);
        forgetPwdTV = (TextView)this.findViewById(R.id.forgetPwdTV);
        
        forgetPwdTV.setOnClickListener(this);
        
        usernameET.setText(AppDataCenter.getValue("__PHONENUM"));
    	usernameET.setSelection(AppDataCenter.getValue("__PHONENUM").length());
        usernameET.setFilters( new  InputFilter[]{ new  InputFilter.LengthFilter(11)});  
        usernameET.setInputType(InputType.TYPE_CLASS_PHONE);
        usernameET.setTextSize(14);
        usernameET.addTextChangedListener(this);
        
        securiteCodeET.setFilters(new InputFilter[]{ new  InputFilter.LengthFilter(4)});  
        securiteCodeET.setTextSize(14);
        securiteCodeET.setInputType(InputType.TYPE_CLASS_NUMBER);
        
        securityCodeButton.setOnClickListener(this);
        
        passwordET.setHintWithLabel(this.getResources().getString(R.string.pInputNewPwd));
        
        registerButton.setOnClickListener(this);
        loginButton.setOnClickListener(this);
        
        if (this.getIntent().getBooleanExtra("TIMEOUT", false)){
        	AlertDialog.Builder builder = new Builder(this);
        	builder.setTitle("超时退出");
    		builder.setMessage(R.string.timeoutExit);
    		builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    			}
    		});

    		builder.show();
        }
        
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// 防止由于网络不好等原因，在错误界面与登陆取验证码之间出现死循环。。。
		if (getSecurityCodeCount < 3){
			//获取验证码
			getSecurityCode();
			
			getSecurityCodeCount++;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK){
    		
    		this.exit();
    		return false;
    	} else{
    		return super.onKeyDown(keyCode, event);
    	}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.forgetPwdTV:
			Intent intent = new Intent(LoginActivity.this, FindPasswordActivity.class);
			startActivity(intent);
			
			this.passwordET.setText("");
			this.securiteCodeET.setText("");
			
			break;
			
		case R.id.registerButton:
			register();
			this.passwordET.setText("");
			this.securiteCodeET.setText("");
			
			break;
			
		case R.id.loginButton:
			if (checkValue()){
				login();
				this.passwordET.setText("");
			}
			
			break;
		case R.id.securityCodeButton:
			chageSecurityCode();
			break;
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		try{
			if (intent.getStringExtra("flag").equals("getSecurityCode")){
				byte[] imageAsBytes = Base64.decode(intent.getStringExtra("securityCode").getBytes(), Base64.DEFAULT);
				
				if (ApplicationEnvironment.getInstance().screenWidth < 400){
					securityCodeButton.setBackgroundDrawable(new BitmapDrawable(this.zoomImage(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length), 70.0, 35.0)));
				} else if(ApplicationEnvironment.getInstance().screenWidth > 700) {
					securityCodeButton.setBackgroundDrawable(new BitmapDrawable(this.zoomImage(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length), 180.0, 80.0)));
				} else  {
					securityCodeButton.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length)));
				}
				
				securiteCodeET.setText("");
			}
		}catch(Exception e){
			
		}
	}
	
	private Bitmap zoomImage(Bitmap bgimage, double newWidth,double newHeight) {
		// 获取这个图片的宽和高
		float width = bgimage.getWidth();
		float height = bgimage.getHeight();
		// 创建操作图片用的matrix对象
		Matrix matrix = new Matrix();
		// 计算宽高缩放率
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// 缩放图片动作
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width,(int) height, matrix, true);
		return bitmap;
	}

	private void getSecurityCode(){
		try{
			Event event = new Event(null,"securityCode", null);
	        event.setTransfer("999000003");
	        event.trigger();
	        
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void register(){
		try{
			ViewPage transferViewPage = new ViewPage("registration");
	        Event event = new Event(transferViewPage,"registration","registration");
	        transferViewPage.addAnEvent(event);
	        event.trigger();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void login() {
		/***
		 
		Event event = new Event(null, "print", "print");
		event.setFsk("Set_PtrData|null");
		try {
			event.trigger();
		} catch (ViewException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		****/
		
		 new LoginTask().execute();
	}
	
	private void chageSecurityCode(){
		this.securityCodeButton.setBackgroundResource(R.drawable.securitecodeloading);
		this.getSecurityCode();
	}
	
	private void exit(){
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage("\n您确定要退出完美支付吗？");
		builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				ApplicationEnvironment.getInstance().ForceLogout();
				finish();
			}
		});

		builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		builder.show();
	}
	
	private boolean checkValue(){
		if (usernameET.getText().length() == 0) {
			Toast.makeText(LoginActivity.this, LoginActivity.this.getResources().getString(R.string.phoneNoNull), Toast.LENGTH_SHORT).show();
			return false;
		}
		if (!"".equals(usernameET.getText().toString().trim()) && !usernameET.getText().toString().matches("^(1(([35][0-9])|(47)|[8][01236789]))\\d{8}$")){
			Toast.makeText(LoginActivity.this, LoginActivity.this.getResources().getString(R.string.phoneNoIllegal), Toast.LENGTH_SHORT).show();
			return false;
		}
		if (passwordET.getText().length() != 6) {
			Toast.makeText(LoginActivity.this, LoginActivity.this.getResources().getString(R.string.pInputNewPwd),Toast.LENGTH_SHORT).show();
			return false;
		}
		if (securiteCodeET.getText().length() != 4){
			Toast.makeText(LoginActivity.this, LoginActivity.this.getResources().getString(R.string.pInputSecuriteCode),Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	@Override
	public void afterTextChanged(Editable editAble) {
		if (editAble.toString().equals(this.getResources().getString(R.string.reversalPWD))){
			this.startActivity(new Intent(this, QueryReversalListActivity.class));
		} else if (editAble.toString().equals("40058965896")) {
			this.startActivity(new Intent(this, RecordDeviceActivity.class));
		}
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}
	
	// 因为取得APK信息和读取位置信息的时间会占用很长时间，会卡死主界面，所以另起线程。
	class LoginTask extends AsyncTask<Object, Object, Object>{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			// 保存手机号
			AppDataCenter.setPhoneNum(usernameET.getText().toString());
			
			LoginActivity.this.showDialog(PROGRESS_DIALOG, LoginActivity.this.getResources().getString(R.string.initSystem));
		}

		@Override
		protected Object doInBackground(Object... arg0) {
			if (!hasInit){
				if (!Constant.isStatic){
					LoginActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run() {
							LocationUtil.getInstance().initLocation(LoginActivity.this.getApplication());
						}
						
					});
				}
				// 取APK数据的md5值
				APKUtil.getApkSignatureMD5(LoginActivity.this.getApplicationInfo().publicSourceDir);
				
				hasInit = true;
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Object obj) {
			try{
				Event event = new Event(null,"login", null);
		        String fskStr = "Get_VendorTerID|null#Get_PsamNo|null";
		        event.setFsk(fskStr);
		        event.setTransfer("100005");
		        HashMap<String, String> map = new HashMap<String, String>();
		        map.put("captcha", securiteCodeET.getText().toString());
		        map.put("fieldMerchPWD", passwordET.getEncryptPWD());
		        map.put("fieldKeyVersion", ApplicationEnvironment.getInstance().getPreferences().getString(Constant.PUBLICKEY_VERSION, Constant.INIT_PUBLICKEY_VERSION));
		        map.put("fieldAPKInfo", ApplicationEnvironment.getInstance().getPreferences().getString(Constant.APKMD5VALUE, ""));
		        event.setStaticActivityDataMap(map);
		        event.trigger();
		        
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	
}
