package com.dhc.pos.agent.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.dhc.pos.activity.BaseActivity;
import com.dhc.pos.activity.LoginActivity;
import com.dhc.pos.activity.RecordDeviceActivity;
import com.dhc.pos.activity.SettlementSuccessActivity;
import com.dhc.pos.activity.TimeoutService;
import com.dhc.pos.agent.client.db.AnnouncementDBHelper;
import com.dhc.pos.agent.client.db.ReversalDBHelper;
import com.dhc.pos.agent.client.db.TransferSuccessDBHelper;
import com.dhc.pos.agent.client.db.UploadSignImageDBHelper;
import com.dhc.pos.dynamic.core.Event;
import com.dhc.pos.dynamic.core.ViewPage;
import com.dhc.pos.fsk.FSKOperator;
import com.dhc.pos.model.AnnouncementModel;
import com.dhc.pos.model.FieldModel;
import com.dhc.pos.model.TransferModel;
import com.dhc.pos.model.TransferSuccessModel;
import com.dhc.pos.util.AssetsUtil;
import com.dhc.pos.util.PhoneUtil;
import com.dhc.pos.util.StringUtil;

public class TransferLogic {
	
	private static final String GENERALTRANSFER							= "GENERALTRANSFER";
	private static final String UPLOADSIGNIMAGETRANSFER					= "UPLOADSIGNIMAGETRANSFER";
	
	public static TransferLogic instance 								= null;
	
	private HashMap<String, TransferPacketThread> transferMap 			= null;
	
	public static TransferLogic getInstance(){
		if (null == instance){
			instance = new TransferLogic();
		}
		
		return instance;
	}
	
	public TransferLogic(){
		transferMap = new HashMap<String, TransferPacketThread>();
	}
	
	// 动态机制通过此方法执行相应的逻辑。
	public void transferAction(String transferCode, HashMap<String, String> map){
		
		Handler transferHandler = new Handler(){
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				// 只能处理且只用处理正确的消息
				switch(msg.what){
				case 0:
					actionDone((HashMap<String, String>)msg.obj);
					break;
				}
			}
		};
		
		/**进行逻辑处理**/
		TransferPacketThread thread = new TransferPacketThread(transferCode, map, transferHandler);
		
		if (transferCode.equals("500000001")){ // 签购单上传
			transferMap.put(TransferLogic.UPLOADSIGNIMAGETRANSFER, thread);
		} else {
			transferMap.put(TransferLogic.GENERALTRANSFER, thread);
		}
		
		thread.start();
	}
	
	private void actionDone(HashMap<String, String> fieldMap){
		String transferCode = fieldMap.get("fieldTrancode");
		
		if ("080000".equals(transferCode)){ // 签到
			this.signDone(fieldMap);
			
		} else if("500201".equals(transferCode)){ // 批结算
			this.settlementDone(fieldMap);
			
		} else if ("082001".equals(transferCode)){ // 签退
			this.signOffDone(fieldMap);
			
		} else if ("100001".equals(transferCode)){ // 商户注册
			this.registrDone(fieldMap);
			
		} else if("100005".equals(transferCode)){ // 检验商户密码 登陆
			this.loginDone(fieldMap);
			
		} else if("020001".equals(transferCode)){ // 银行卡余额查询
			this.queryBalanceDone(fieldMap);
			
		} else if("020022".equals(transferCode)){ // 收款
			this.receiveTransDone(fieldMap);
			
		} else if ("020023".equals(transferCode)){ // 收款撤销
			this.revokeTransDone(fieldMap);
			
		} else if (AppDataCenter.getReversalMap().containsValue(transferCode)){ // 冲正
			gotoCommonSuccessActivity(fieldMap.get("fieldMessage"));
			
		} else if("200001111".equals(transferCode)){ // 银行卡付款
			this.bankTransferDone(fieldMap);
			
		} else if("500000001".equals(transferCode)){ // 签购单上传
			this.uploadReceiptDone(fieldMap);
			
		} else if("600000001".equals(transferCode)){
			this.queryHistoryGroupDone(fieldMap);
			
		} else if("600000002".equals(transferCode)){
			this.queryHistoryListDone(fieldMap);
			
		} else if("999000001".equals(transferCode)){
			this.downloadAnnouncements(fieldMap);
			
		} else if("999000002".equals(transferCode)){
			this.checkUpdateAPK(fieldMap);
			
		} else if("999000003".equals(transferCode)){
			this.downloadSecurityCode(fieldMap);
			
		} else {
			gotoCommonSuccessActivity(fieldMap.get("fieldMessage"));
		}
	}
	

	/**
	 * 登陆
	 */
	private void loginDone(HashMap<String, String> fieldMap) {
		Editor editor = ApplicationEnvironment.getInstance().getPreferences().edit();
		
		try {
			
			if (null != fieldMap) {
				
				// 先判断公钥是否需要更新
				if (fieldMap.get("keyFlag").equals("1")){ // 0-不需更新 1-需要更新
					// 更新公钥信息
					String[] keys = parsePublickey(fieldMap.get("field62"));
					
					editor.putString(Constant.PUBLICKEY_MOD, keys[0]);
					editor.putString(Constant.PUBLICKEY_EXP, keys[1]);
					editor.putString(Constant.PUBLICKEY_VERSION, fieldMap.get("fieldnewVersion"));
					editor.putString(Constant.PUBLICKEY_TYPE, fieldMap.get("publicKeyType"));
					
					this.gotoCommonFaileActivity("由于安全密钥已更新，请您重新登陆");
					return;
				}
				
				// 取服务器最新的公告编号
				if (fieldMap.containsKey("max_id_notice")){
					editor.putString(Constant.SERVER_ANNOUNCEMENT_LASTEST_NUM, fieldMap.get("max_id_notice"));
				}
				
				// 保存商户名称
				if (fieldMap.containsKey("merchName")){
					editor.putString(Constant.MERCHERNAME, fieldMap.get("merchName"));
				}
				
				// 检查版本，如有可能下载文件进行更新。
				if (!Constant.isStatic) {
					int currentVersion = ApplicationEnvironment.getInstance().getPreferences().getInt(Constant.VERSION, 0);
					if(UpdateClient.updateFiles(currentVersion,fieldMap.get("fieldVersion"), fieldMap.get("fieldFileName"))){
						Log.e("登陆", "下载更新文件完成或不需要更新");
					} else {
						this.gotoCommonFaileActivity("更新文件失败");
						return;
					}
				}
				
				// 读取有可能更新后的系统配置信息。systemconfig.xml
				if(loadSystemConfig()){
					Log.e("登陆", "读取系统配置文件完成");
				} else {
					this.gotoCommonFaileActivity("读取系统配置文件失败");
					return;
				}
				
				// 启动超时退出服务
				Intent intent = new Intent(BaseActivity.getTopActivity(), TimeoutService.class);
				BaseActivity.getTopActivity().startService(intent);
				
				// BaseActivity.getTopActivity().startService(new Intent("com.dhc.pos.timeoutService"));
				
				// 登陆成功，跳转到菜单界面
				BaseActivity.getTopActivity().showDialog(BaseActivity.PROGRESS_DIALOG, "登录成功");
				BaseActivity.getTopActivity().startActivity(new Intent("com.dhc.pos.lrcatalog"));
				BaseActivity.getTopActivity().finish();
				
			} else {
				this.gotoCommonFaileActivity("服务器返回异常");
			}
			
		} catch (Exception e) {
			this.gotoCommonFaileActivity("服务器返回异常");
			e.printStackTrace();
		} finally{
			editor.commit();
		}
		
	}
	
	/**
	 * 签到
	 * 
	 * 当点击签到按纽后并从服务器返回JSON后执行此方法。
	 * 
	 * 执行此方法说明服务器端签到正常，没有其他的异常情况发生。
	 * 签到成功后
	 * 1、首先要更新工作密钥。按长度分别切割
	 * 2、然后将收款撤销表清空。
	 */
	private void signDone(final HashMap<String, String> fieldMap){
		// 更新批次号
		String batchNum = fieldMap.get("field60").replace(" ", "").substring(2, 8); // 60域不带长度信息
		AppDataCenter.setBatchNum(batchNum);
		
		// 清空上一个批次的交易成功的信息，即用于消费撤销和查询签购单的数据库表
		TransferSuccessDBHelper helper = new TransferSuccessDBHelper();
		if (helper.deleteTransfers()){
			Log.e("debug", "更换批次后 成功 清空需清除的成功交易！");
		} else {
			Log.e("debug", "更换批次后清空需清除的成功交易 失败 ！");
		}
		
		// 根据62域字符串切割得到工作密钥
		String newKey = fieldMap.get("field62").replace(" ", "");
		String pinKey = null;
		String macKey = null;
		String stackKey = null;
		
		try{
			if (null != newKey && !"".equals(newKey)){
				
				// 标准
				pinKey = newKey.substring(0, 40);
				macKey = newKey.substring(40, 56) + newKey.substring(72);
				stackKey = pinKey;
				
			} else { 
				return;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		//保存工作密钥
		Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
				case 0:
					BaseActivity.getTopActivity().hideDialog(BaseActivity.PROGRESS_DIALOG);
					// 签到没有传fieldMessage
					//TransferLogic.getInstance().gotoCommonSuccessActivity(fieldMap.containsKey("fieldMessage")?fieldMap.get("fieldMessage"):"签到成功\n\n[设备已成功更新工作密钥]");
					TransferLogic.getInstance().gotoCommonSuccessActivity("签到成功\n\n[设备已成功更新工作密钥]");
					break;
				}
			}
			
		};
		
		StringBuffer sb = new StringBuffer();
		sb.append("Get_RenewKey|string:").append(pinKey).append(",string:").append(macKey).append(",string:").append(stackKey);
		FSKOperator.execute(sb.toString(), handler);
	}
	
	/**
	 * 结算
	 * 
	 */
	private void settlementDone(HashMap<String, String> fieldMap){
		try{
			String field48 = fieldMap.get("field48");
			String debitAmount = field48.substring(0, 12);
			String debitCount = field48.substring(12, 15);
			String creditAmount = field48.substring(15, 27);
			String creditCount = field48.substring(27, 30);
			
			HashMap<String, String> tempMap = new HashMap<String, String>();
			tempMap.put("fieldMessage", fieldMap.get("fieldMessage"));
			tempMap.put("debitAmount", StringUtil.String2SymbolAmount(debitAmount));
			tempMap.put("debitCount", debitCount);
			tempMap.put("creditAmount", StringUtil.String2SymbolAmount(creditAmount));
			tempMap.put("creditCount", creditCount);
			
			Intent intent = new Intent(BaseActivity.getTopActivity(), SettlementSuccessActivity.class);
			intent.putExtra("map", tempMap);
			BaseActivity.getTopActivity().startActivityForResult(intent, 0);
			
		} catch(Exception e){
			this.gotoCommonFaileActivity("结算统计失败，请重试");
		}
		
	}
	
	
	/**
	 * 签退
	 */
	private void signOffDone(HashMap<String, String> fieldMap){
		gotoCommonSuccessActivity(fieldMap.get("fieldMessage"));
	}

	/**
	 * 注册
	 */
	private void registrDone(HashMap<String, String> fieldMap){
		// 保存手机号
		AppDataCenter.setPhoneNum(transferMap.get(GENERALTRANSFER).getSendMap().get("termMobile"));
		
		if (fieldMap == null || !fieldMap.containsKey("field62") || !fieldMap.containsKey("merchName") || !fieldMap.containsKey("publicKeyVersion") || !fieldMap.containsKey("publicKeyType")){
			this.gotoCommonFaileActivity("服务器返回数据异常，请联系客服！");
			return;
		}
		
		// 注册成功后，保存商户名称，保存公钥信息，然后跳转到设置密码界面
		String[] keys = parsePublickey(fieldMap.get("field62"));
		
		Editor editor = ApplicationEnvironment.getInstance().getPreferences().edit();
		editor.putString(Constant.MERCHERNAME, fieldMap.get("merchName"));
		editor.putString(Constant.PUBLICKEY_MOD, keys[0]);
		editor.putString(Constant.PUBLICKEY_EXP, keys[1]);
		editor.putString(Constant.PUBLICKEY_VERSION, fieldMap.get("publicKeyVersion"));
		editor.putString(Constant.PUBLICKEY_TYPE, fieldMap.get("publicKeyType"));
		editor.commit();
		
		try{
			ViewPage transferViewPage = new ViewPage("setpassword");
	        Event event = new Event(transferViewPage,"setpassword","setpassword");
	        transferViewPage.addAnEvent(event);
	        event.trigger();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 解析公钥字符串，得到mod exp
	 * 
	 * @param key 公钥的ascii
	 * @return mod exp 数组
	 */
	private String[] parsePublickey(String key){
		// 计算key的字节长度并保存到数组
		String[] bytes = new String[key.length() / 2];
		for (int i = 0; i < key.length() / 2; i++) {
			bytes[i] = key.substring(i * 2, i * 2 + 2);
		}
		// Byte Length
		int index = 1;
		int length = Integer.parseInt(bytes[index], 16);
		if (length > 128)
			index += length - 128;

		// Modulus Length
		index += 2;
		length = Integer.parseInt(bytes[index], 16);
		if (length > 128) {
			int i = length - 128;
			String lenStr = "";
			for (int j = index + 1; j < index + i + 1; j++)
				lenStr += bytes[j];

			index += i;
			length = Integer.parseInt(lenStr, 16);
		}

		// 保存mod值
		StringBuffer modBuff = new StringBuffer();
		for (int i = index + 1; i < index + 1 + length; i++)
			modBuff.append(bytes[i]);

		// Exponent Length
		index += length + 2;
		length = Integer.parseInt(bytes[index], 16);
		if (length > 128) {
			int i = length - 128;
			String lenStr = "";
			for (int j = index + 1; j < index + i + 1; j++)
				lenStr += bytes[j];

			index += i;
			length = Integer.parseInt(lenStr, 16);
		}
		
		// 保存exponent值
		index += 1;
		StringBuffer expBuff = new StringBuffer();
		for (int i = index; i < index + length; i++)
			expBuff.append(bytes[i]);

		return new String[]{modBuff.toString(), expBuff.toString()};
	}
	
	/**
	 * 这是第一次以后的查询交易明细，直接从第一次的请求报文中拿出所有的值，以后只是替换欲请求的页码。五个值包括密码也直接从原请求报文中取值
	 * @param currentPage 要请求的当前页面
	 */
	public void queryHistoryAction(String currentPage){
		HashMap<String, String> sendMap = transferMap.get(GENERALTRANSFER).getSendMap();
		// 验证上次请求的信息是否还存在，如果存在则直接更新上次的请求页面直接请求数据，否则失败让用户重新操作

		if (null != sendMap && sendMap.containsKey("fieldTrancode")){
			if (sendMap.containsKey("field11")){
				sendMap.put("TRACEAUDITNUM", sendMap.get("field11")); // 还是使用一个流水号
			}
			sendMap.put("pageNo", currentPage); // 替换查询页码
			this.transferAction(sendMap.get("fieldTrancode"), sendMap);
		} else {
			TransferLogic.getInstance().gotoCommonFaileActivity("查询明细时出现异常，请重试！");
		}
	}
	
	private void queryHistoryGroupDone(HashMap<String, String> fieldMap){
		HashMap<String, String> sendMap = transferMap.get(GENERALTRANSFER).getSendMap();
		if (null != sendMap && sendMap.containsKey("BeginDate") && sendMap.containsKey("EndDate")){
			Intent intent = new Intent("com.dhc.pos.queryTransferHistoryGroupActivity");
			intent.putExtra("detail", fieldMap.get("detail"));
			intent.putExtra("BeginDate", sendMap.get("BeginDate"));
			intent.putExtra("EndDate", sendMap.get("EndDate"));
			BaseActivity.getTopActivity().startActivity(intent);
		} else {
			TransferLogic.getInstance().gotoCommonFaileActivity("查询明细时出现异常，请重试！");
		}
	}
	
	private void queryHistoryListDone(HashMap<String, String> fieldMap){
		HashMap<String, String> sendMap = transferMap.get(GENERALTRANSFER).getSendMap();
		if (null != sendMap && sendMap.containsKey("totalCount")){
			Intent intent = new Intent("com.dhc.pos.queryTransferHistoryList");
			intent.putExtra("map", fieldMap);
			intent.putExtra("totalCount", Integer.parseInt(sendMap.get("totalCount")));
			BaseActivity.getTopActivity().startActivity(intent);
		} else {
			TransferLogic.getInstance().gotoCommonFaileActivity("查询明细时出现异常，请重试！");
		}
	}

	/**
	 * 银行卡余额查询
	 */
	private void queryBalanceDone(final HashMap<String, String> fieldMap){
		Intent intent = new Intent(ApplicationEnvironment.getInstance().getApplication().getPackageName()+".showBalance");
		intent.putExtra("balance", fieldMap.get("field54"));
		intent.putExtra("availableBalance", fieldMap.get("field4"));
		intent.putExtra("accountNo", fieldMap.get("field2"));
		intent.putExtra("message", fieldMap.get("fieldMessage"));
		BaseActivity.getTopActivity().startActivityForResult(intent, 0);
	}
	
	
	/**
	 * 收款或收款撤销成功，记录数据以备查询签购单
	 */
	private void recordSuccessTransfer(HashMap<String, String> fieldMap){
		TransferSuccessModel model = new TransferSuccessModel();
		model.setAmount(fieldMap.get("field4"));
		model.setTraceNum(fieldMap.get("field11"));
		model.setTransCode(fieldMap.get("fieldTrancode"));
		model.setDate(fieldMap.get("field13"));
		model.setContent(fieldMap);
		TransferSuccessDBHelper helper = new TransferSuccessDBHelper();
		boolean flag = helper.insertATransfer(model);
		if (!flag){
			Log.e("DATABASE", "成功交易写入数据库时操作失败。。。");
		}
	}
	
	/**
	 * 收款
	 */
	private void receiveTransDone(HashMap<String, String> fieldMap){
		recordSuccessTransfer(fieldMap);
		
		
		try{
			// 旧 001903   新 001917
			if (AppDataCenter.getValue("__TERSERIALNO").startsWith("001903")) { // 001903
				// 打印
				Intent intent = new Intent("com.dhc.pos.printReceipt");
				intent.putExtra("content", fieldMap);
				BaseActivity.getTopActivity().startActivityForResult(intent, 0);
				
			} else {
				ViewPage transferViewPage = new ViewPage("transfersuccess");
		        Event event = new Event(transferViewPage,"transfersuccess","transfersuccess");
		        event.setStaticActivityDataMap(fieldMap);
		        transferViewPage.addAnEvent(event);
		        event.trigger();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
		/***
		try{
			SharedPreferences sp = ApplicationEnvironment.getInstance().getPreferences();
			String deviceIDs = sp.getString(Constant.DEVICEID, "");
			String[] deviceArray = deviceIDs.split("\\|");
			for (String str : deviceArray) {
				if (!str.equals("") && str.equals(AppDataCenter.getValue("__TERSERIALNO").substring(13))){
					ViewPage transferViewPage = new ViewPage("transfersuccess");
			        Event event = new Event(transferViewPage,"transfersuccess","transfersuccess");
			        event.setStaticActivityDataMap(fieldMap);
			        transferViewPage.addAnEvent(event);
			        event.trigger();
			        
			        return;
				}
			}
			
			// 打印
			Intent intent = new Intent("com.dhc.pos.printReceipt");
			intent.putExtra("content", fieldMap);
			BaseActivity.getTopActivity().startActivityForResult(intent, 0);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		****/
	}
	
	/**
	 * 收款撤销
	 */
	private void revokeTransDone(HashMap<String, String> fieldMap){
		recordSuccessTransfer(fieldMap);
		
		TransferSuccessDBHelper helper = new TransferSuccessDBHelper();
		helper.updateRevokeFalg(fieldMap.get("field11"));
		
		try{
			if (AppDataCenter.getValue("__TERSERIALNO").startsWith("001903")) { //001903
				// 打印
				Intent intent = new Intent("com.dhc.pos.printReceipt");
				intent.putExtra("content", fieldMap);
				BaseActivity.getTopActivity().startActivityForResult(intent, 0);
				
			} else {
				ViewPage transferViewPage = new ViewPage("transfersuccess");
		        Event event = new Event(transferViewPage,"transfersuccess","transfersuccess");
		        event.setStaticActivityDataMap(fieldMap);
		        transferViewPage.addAnEvent(event);
		        event.trigger();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		
		/***
		try{
			SharedPreferences sp = ApplicationEnvironment.getInstance().getPreferences();
			String deviceIDs = sp.getString(Constant.DEVICEID, "");
			String[] deviceArray = deviceIDs.split("\\|");
			for (String str : deviceArray) {
				if (!str.equals("") && str.equals(AppDataCenter.getValue("__TERSERIALNO").substring(13))){
					ViewPage transferViewPage = new ViewPage("transfersuccess");
			        Event event = new Event(transferViewPage,"transfersuccess","transfersuccess");
			        event.setStaticActivityDataMap(fieldMap);
			        transferViewPage.addAnEvent(event);
			        event.trigger();
			        
			        return;
				}
			}
			
			// 打印
			Intent intent = new Intent("com.dhc.pos.printReceipt");
			intent.putExtra("content", fieldMap);
			BaseActivity.getTopActivity().startActivityForResult(intent, 0);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		***/
	}
	
	/**
	 * 银行账户转账 (付款)
	 */
	private void bankTransferDone(HashMap<String, String> fieldMap){
		Intent intent = new Intent(ApplicationEnvironment.getInstance().getApplication().getPackageName() + ".transferSuccessSendSms");
		intent.putExtra("map", fieldMap);
		BaseActivity.getTopActivity().startActivityForResult(intent, 0);
	}
	
	/**
	 * 冲正
	 */
	 public boolean reversalAction(){
		 return false;
		 
		 /*
		 if (Constant.isStatic){
			 return false;
		 }
		 
		 ReversalDBHelper helper = new ReversalDBHelper();
		 HashMap<String, String> map = helper.queryNeedReversal();
		 
		 if (null == map || map.size() == 0){
			 return false;
		 } else {
			 BaseActivity.getTopActivity().showDialog(BaseActivity.PROGRESS_DIALOG, "正在发起冲正交易，请稍候...");
			 
			// 更新冲正表，则冲正次数加1。
			// 注意这可能有问题，因为如果网络不通，直接没有从手机中发出交易，也已经使冲正次数发生变更
			 ReversalDBHelper DBhelper = new ReversalDBHelper();
			 DBhelper.updateReversalCount(map.get("field11"));
			
			 // 将原交易的transferCode改为对应的冲正的transferCode
			 map.put("fieldTrancode", AppDataCenter.getReversalMap().get(map.get("fieldTrancode")));
			 
			 this.transferAction(map.get("fieldTrancode"), map);
			 
			 return true;
		 }
		 */
	 }
	 
	 /**
	  * 签购单上传
	  * 
	  * 上传签购单接口也用于只传送手机号，如转账
	  */
	 public void uploadReceiptAction(HashMap<String, String> fieldsMap){
		// 静态演示模式下不上传签购单。
		 if (Constant.isStatic){
			 	BaseActivity.getTopActivity().setResult(BaseActivity.RESULT_OK);
			 	BaseActivity.getTopActivity().finish();
				
			 	
			 	return;
		 }
		 
		 
		 
		 try{
	        HashMap<String, String> map = new HashMap<String, String>();
	        map.put("field41", AppDataCenter.getValue("__TERID"));
	        map.put("field42", AppDataCenter.getValue("__VENDOR"));
	        map.put("termMobile", AppDataCenter.getValue("__PHONENUM"));
	        map.put("ReaderID", AppDataCenter.getValue("__TERSERIALNO"));
	        map.put("PSAMID", AppDataCenter.getValue("__PSAMNO"));
	        
	        map.put("field7", transferMap.get(GENERALTRANSFER).getSendMap().get("field7"));
	        map.put("field11", fieldsMap.get("field11"));
	        map.put("batchNum", fieldsMap.get("field60").substring(2, 8));
	        map.put("filedIMEI", fieldsMap.get("imei"));
	        map.put("fieldMobile", fieldsMap.get("receivePhoneNo"));
	        
	        
	        if (fieldsMap.containsKey("signImageName")){
	        	String imagePath = Constant.SIGNIMAGESPATH + fieldsMap.get("signImageName") + ".JPEG";
	        	map.put("fieldImage", StringUtil.Image2Base64(imagePath));
	        	
	        	// 删除签名图片
	        	File f = new File(imagePath);
				if (f.exists()){
					if (f.delete()){
						Log.e("SignImage", "文件删除成功！");
					} else {
						Log.e("SignImage", "文件删除失败！");
					}
				} else {
					 Log.e("SignImage", "签名图片不存在！");
				 }
	        } else {
	        	map.put("fieldImage", "");
	        }
	        
	        // 写入数据库
	        UploadSignImageDBHelper helper = new UploadSignImageDBHelper();
	        if(helper.insertATransfer(fieldsMap.get("field11"), fieldsMap.get("receivePhoneNo"), map)){
	        	Log.e("sign image", "成功写入数据库");
	        } else {
	        	Log.e("sign image", "写入数据库失败");
	        }
	        
		}catch(Exception e){
			e.printStackTrace();
			
		} finally{
			try{
				BaseActivity.getTopActivity().startService(new Intent("com.dhc.pos.uploadSignImageService"));
				
				BaseActivity.getTopActivity().setResult(BaseActivity.RESULT_OK);
				BaseActivity.getTopActivity().finish();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	 }
	 
	 /**
	  * 签购单上传完成
	  */
	 private void uploadReceiptDone(HashMap<String, String> fieldMap){
		 String field11 = transferMap.get(UPLOADSIGNIMAGETRANSFER).getSendMap().get("field11");
		 
		 UploadSignImageDBHelper helper = new UploadSignImageDBHelper();
		 
		 
		// 发送短信
		 String receMobile = helper.queryReceMobile(field11);
		 if (SystemConfig.isSendSMS() && !"".equals(receMobile)){
			 PhoneUtil.sendSMS(receMobile, fieldMap.get("field44"));
		 }
		 
		 // 签购单上传成功更新数据库
		 helper.updateUploadFlagSuccess(field11);
		 
		 // gotoCommonSuccessActivity(fieldMap.get("fieldMessage"));
	 }
	 
	 /**
	  * 正在下载新公告 
	  */
	 private void downloadAnnouncements(HashMap<String, String> fieldMap) {
		 try{
			 int currentAnnouncementLastestNum = Integer.parseInt(ApplicationEnvironment.getInstance().getPreferences().getString(Constant.SYSTEM_ANNOUNCEMENT_LASTEST_NUM, "0"));
			 
			 AnnouncementDBHelper helper = new AnnouncementDBHelper();
			 
			 String fieldContent = fieldMap.get("content_notice");
			 
			 if (null != fieldContent && !fieldContent.trim().equals("")) {
				 String[] contentArray = fieldContent.split("\\|");
					for (String content : contentArray){
						HashMap<String, String> map = new HashMap<String, String>();
						String[] tempArray = content.split("\\^");
						for (String str : tempArray){
							String[] fieldArray = str.split(":");
							if (fieldArray.length == 1){
								map.put(fieldArray[0], "");
							} else if (fieldArray.length == 2){
								map.put(fieldArray[0], fieldArray[1]);
							}
						}
						
						if (map.containsKey("id_notice")){
							AnnouncementModel model = new AnnouncementModel(map);
							helper.insertAnnouncement(model);
							
							// 更新最新公告编号
							int noticeNum = Integer.parseInt(map.get("id_notice"));
							if (noticeNum > currentAnnouncementLastestNum){
								Editor editor = ApplicationEnvironment.getInstance().getPreferences().edit();
								editor.putString(Constant.SYSTEM_ANNOUNCEMENT_LASTEST_NUM, map.get("id_notice"));
								editor.commit();
							}
							
						} else{
							TransferLogic.getInstance().gotoCommonFaileActivity("更新公告时出现异常，请重试。");
							return;
						}
						
					}
			 	}
				
			 BaseActivity.getTopActivity().startActivity(new Intent("com.dhc.pos.announcementListActivity"));
				
		 } catch(Exception e){
			 e.printStackTrace();
			 TransferLogic.getInstance().gotoCommonFaileActivity("更新公告时出现异常，请重试！");
		 }
		 
	 }
	 
	 /**
	  * 检查更新
	  */
	 private void checkUpdateAPK(HashMap<String, String> fieldMap) {
		 Intent intent = new Intent("com.dhc.pos.updateAPKService");
		 intent.putExtra("flag", "response");
		 intent.putExtra("apkName", fieldMap.get("version_name"));
		 intent.putExtra("serverVersionCode", fieldMap.get("version_number"));
		 BaseActivity.getTopActivity().startService(intent);
	 }
	 
	 /**
	  * 取验证码 
	  */
	 private void downloadSecurityCode(HashMap<String, String> fieldMap) {
		 LoginActivity.getSecurityCodeCount = 0;
		 
		 // 跳转到哪一个页面不确定。注意一定要在欲跳转的页面设置setAction("');
		 Intent intent = new Intent(BaseActivity.getTopActivity().getIntent().getAction());
		 intent.putExtra("flag", "getSecurityCode");
		 intent.putExtra("securityCode", fieldMap.get("captcha"));
		 BaseActivity.getTopActivity().startActivity(intent);
		 BaseActivity.getTopActivity().hideDialog(BaseActivity.PROGRESS_DIALOG);
	 }
	 
	 public TransferModel parseConfigXML(String confName) throws FileNotFoundException{
		TransferModel transfer = new TransferModel();
		
		InputStream stream = null;
		
		FieldModel field = null;
		try {
			stream = AssetsUtil.getInputStreamFromPhone(confName);
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("加载系统文件异常("+confName+")");
		}
		
		try{
			KXmlParser parser = new KXmlParser(); 
			parser.setInput(stream,"utf-8");
			// 获取事件类型
	        int eventType = parser.getEventType();
	        while(eventType!=XmlPullParser.END_DOCUMENT){  
	            switch(eventType){  
	            case XmlPullParser.START_TAG:
	            	if ("root".equalsIgnoreCase(parser.getName())){
	            		transfer.setShouldMac(parser.getAttributeValue(null, "shouldMac"));
	            		
	            	} else if("field".equalsIgnoreCase(parser.getName())){
	                    field = new FieldModel();  
	                    field.setKey(parser.getAttributeValue(null, "key"));
	                	field.setValue(parser.getAttributeValue(null, "value"));
	                	//field.setMacField(parser.getAttributeValue(null, "macField"));
	                }  
	                 
	                break;
	            case XmlPullParser.END_TAG:  
	                if("field".equalsIgnoreCase(parser.getName())){
	                	transfer.addField(field);
	                }  
	                break;  
	            }  
	            eventType = parser.next();//进入下一个元素并触发相应事件  
	        }
	        
			
		} catch(IOException e){
			e.printStackTrace();
			
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			
		} finally{
				try {
					if(null != stream)
						stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		
		return transfer;
	}
	
	/**
	 * 跳转到通用的成功界面，只显示一行提示信息
	 */
	public void gotoCommonSuccessActivity(String prompt){
		if (null == prompt || "".equals(prompt.trim())){
			prompt = "交易成功";
		}
		
		try{
			ViewPage transferViewPage = new ViewPage("tradesuccess");
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("message", prompt);
	        Event event = new Event(transferViewPage,"tradesuccess","tradesuccess");
	        event.setStaticActivityDataMap(map);
	        transferViewPage.addAnEvent(event);
	        event.trigger();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 跳转到通用的失败界面，只显示一行错误提示信息。
	 */
	public void gotoCommonFaileActivity(String prompt){
		try{
			ViewPage transferViewPage = new ViewPage("tradefailed");
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("failedReason", prompt);
	        Event event = new Event(transferViewPage,"tradefailed","tradefailed");
	        event.setStaticActivityDataMap(map);
	        transferViewPage.addAnEvent(event);
	        event.trigger();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 读取系统配置信息
	 */
	private boolean loadSystemConfig(){
		try{
			InputStream stream = AssetsUtil.getInputStreamFromPhone("systemconfig.xml");
			KXmlParser parser = new KXmlParser(); 
			parser.setInput(stream,"utf-8");
	        int eventType = parser.getEventType();
	        while(eventType!=XmlPullParser.END_DOCUMENT){  
	            switch(eventType){  
	            case XmlPullParser.START_TAG:
	                if("item".equalsIgnoreCase(parser.getName())){
	                	String key = parser.getAttributeValue(null, "key");
	                	if (key.equals("sendSMS")){
	                		SystemConfig.setSendSMS(parser.getAttributeValue(null, "value"));
	                	} else if (key.equals("pageSize")){
	                		SystemConfig.setPageSize(parser.getAttributeValue(null, "value"));
	                	} else if(key.equals("historyInterval")){
	                		SystemConfig.setHistoryInterval(parser.getAttributeValue(null, "value"));
	                	} else if (key.equals("maxReversalCount")){
	                		SystemConfig.setMaxReversalCount(parser.getAttributeValue(null, "value"));
	                	} else if (key.equals("maxTransferTimeout")){
	                		SystemConfig.setMaxTransferTimeout(parser.getAttributeValue(null, "value"));
	                	} else if (key.equals("maxLockTimeout")){
	                		SystemConfig.setMaxLockTimeout(parser.getAttributeValue(null, "value"));
	                	}
	                }  
	                 
	                break;
	            }  
	            eventType = parser.next();//进入下一个元素并触发相应事件  
	        }
	        
	        return true;
	        
		}catch(IOException e){
			e.printStackTrace();
			return false;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return false;
		} 
	}

}
