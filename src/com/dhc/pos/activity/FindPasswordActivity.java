package com.dhc.pos.activity;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.dhc.pos.activity.view.PasswordWithLabelView;
import com.dhc.pos.activity.view.TextWithLabelView;
import com.dhc.pos.agent.client.ApplicationEnvironment;
import com.dhc.pos.dynamic.core.Event;
import com.dhc.pos.R;

// 因为这个页面涉及到验证码，所以做成静态

public class FindPasswordActivity extends BaseActivity implements OnClickListener {
	
	private Button backButton;
	
	private Button findpasswordButton;
	private TextWithLabelView nameET;
	private TextWithLabelView idcardET;
	private PasswordWithLabelView newpasswordET;
	private PasswordWithLabelView againpasswordET;
	private TextWithLabelView securitycodeET;
	private Button securityButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setContentView(R.layout.findpassword);
        
        this.getIntent().setAction("com.dhc.pos.findpassword");
        
        this.findViewById(R.id.topInfoView);
        
        backButton = (Button)this.findViewById(R.id.backButton);
        nameET = (TextWithLabelView)this.findViewById(R.id.nameET);
        idcardET = (TextWithLabelView)this.findViewById(R.id.idcardET);
        newpasswordET = (PasswordWithLabelView)this.findViewById(R.id.newpasswordET);
        againpasswordET = (PasswordWithLabelView)this.findViewById(R.id.againpasswordET);
        securitycodeET = (TextWithLabelView) this.findViewById(R.id.securityET);
        securityButton = (Button) this.findViewById(R.id.securityCodeButton);
        findpasswordButton = (Button)this.findViewById(R.id.findpasswordConfirmButton);
        
        nameET.setHintWithLabel(this.getResources().getString(R.string.name2), this.getResources().getString(R.string.nameNull));
        idcardET.setHintWithLabel(this.getResources().getString(R.string.IDCard), this.getResources().getString(R.string.pInputIDCard));
        idcardET.getEditText().setFilters(new InputFilter[]{new  InputFilter.LengthFilter(18)});  
        newpasswordET.setHintWithLabel(this.getResources().getString(R.string.newPwd), this.getResources().getString(R.string.pInputNewPwd));
        againpasswordET.setHintWithLabel(this.getResources().getString(R.string.newPwdAgain), this.getResources().getString(R.string.pInputNewPwdAgain));
        securitycodeET.setHintWithLabel(this.getResources().getString(R.string.secCode), this.getResources().getString(R.string.pInputSecuriteCode));
        
        securitycodeET.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        
        findpasswordButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        securityButton.setOnClickListener(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//获取验证码
        getSecurityCode();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		try{
			if (intent.getStringExtra("flag").equals("getSecurityCode")){
				byte[] imageAsBytes = Base64.decode(intent.getStringExtra("securityCode").getBytes(), Base64.DEFAULT);

				if (ApplicationEnvironment.getInstance().screenWidth < 400){
					securityButton.setBackgroundDrawable(new BitmapDrawable(this.zoomImage(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length), 70.0, 35.0)));
				} else if(ApplicationEnvironment.getInstance().screenWidth > 700) {
					securityButton.setBackgroundDrawable(new BitmapDrawable(this.zoomImage(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length), 180.0, 80.0)));
				} else  {
					securityButton.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length)));
				}

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
		this.securitycodeET.setText("");
		
		try{
			Event event = new Event(null,"securityCode", null);
	        event.setTransfer("999000003");
	        event.trigger();
	        
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.findpasswordConfirmButton:
				if ("".equals(nameET.getText().trim())){
					Toast.makeText(FindPasswordActivity.this, this.getResources().getString(R.string.nameNull), Toast.LENGTH_SHORT).show();
					break;
				}
				
				Pattern pattern = Pattern.compile("^(\\d{15}$|^\\d{18}$|^\\d{17}(\\d|X|x))$");
				Matcher matcher = pattern.matcher(idcardET.getText().trim());
				if (!matcher.matches()){
					Toast.makeText(FindPasswordActivity.this, this.getResources().getString(R.string.IDCardIllegal), Toast.LENGTH_SHORT).show();
					break;
				}
				
				if (newpasswordET.getText().trim().length() != 6){
					Toast.makeText(FindPasswordActivity.this, this.getResources().getString(R.string.pInputNewPwd), Toast.LENGTH_SHORT).show();
					break;
				}
				
				if (againpasswordET.getText().trim().length() != 6){
					Toast.makeText(FindPasswordActivity.this, this.getResources().getString(R.string.pInputNewPwdAgain), Toast.LENGTH_SHORT).show();
					break;
				}
				
				if (!newpasswordET.getMd5PWD().equals(againpasswordET.getMd5PWD())){
					Toast.makeText(FindPasswordActivity.this, this.getResources().getString(R.string.pwdNotEqual), Toast.LENGTH_SHORT).show();
					break;
				}
				
				if (securitycodeET.getText().trim().length() != 4){
					Toast.makeText(FindPasswordActivity.this, "请输入4位验证码", Toast.LENGTH_SHORT).show();
					break;
				}
				
				
				try{
					Event event = new Event(null,"findPassword", null);
			        String fskStr = "Get_PsamNo|null#Get_VendorTerID|null";
			        event.setFsk(fskStr);
			        event.setTransfer("100003");
			        HashMap<String, String> map = new HashMap<String, String>();
			        map.put("CorpRepr", nameET.getText());
			        map.put("CertificatesId", idcardET.getText()); 
			        map.put("fieldNewPWD", newpasswordET.getEncryptPWD()); 
			        map.put("captcha", this.securitycodeET.getText());
			        
			        event.setStaticActivityDataMap(map);
			        event.trigger();
				}catch(Exception e){
					e.printStackTrace();
				}
				
				break;
				
			case R.id.securityCodeButton:
				this.getSecurityCode();
				break;
				
			case R.id.backButton:
				FindPasswordActivity.this.finish();
				break;
		}
		
	}
}
