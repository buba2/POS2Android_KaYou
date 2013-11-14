package com.dhc.pos.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.dhc.pos.agent.client.ApplicationEnvironment;
import com.dhc.pos.agent.client.Constant;
import com.dhc.pos.agent.client.db.AnnouncementDBHelper;
import com.dhc.pos.dynamic.component.ViewException;
import com.dhc.pos.dynamic.core.Event;
import com.dhc.pos.model.AnnouncementModel;
import com.dhc.pos.R;

/*
 * 先检查是否有新公告信息需要下载，如果没有，直接从数据库中加载数据显示；
 * 如果有新公告，则先下载新公告数据，并写入数据库，然后从数据库中加载公告数据
 */

public class AnnouncementListActivity extends BaseActivity implements OnClickListener, OnItemClickListener {
	
	private Button backButton = null;
	private ListView listView = null;
	
	private ArrayList<AnnouncementModel> modelList = new ArrayList<AnnouncementModel>();
	private ArrayList<HashMap<String, String>> mapList = new ArrayList<HashMap<String, String>>(); 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.trans_list);
		
		this.findViewById(R.id.topInfoView);
		
		((TextView)this.findViewById(R.id.titleView)).setText(this.getResources().getString(R.string.announcementList));
		
		backButton = (Button) this.findViewById(R.id.backButton);
		backButton.setOnClickListener(this);
		
		listView = (ListView) this.findViewById(R.id.transList);
		//生成适配器的Item和动态数组对应的元素  
        SimpleAdapter listItemAdapter = new SimpleAdapter(this, mapList,
            R.layout.announcement_listitem, 
            new String[] {"title","content", "date"},   
            //ImageItem的XML文件里面的一个ImageView,两个TextView ID  
            new int[] {R.id.annTitle, R.id.annContent, R.id.annDate}  
        );             
       
        listView.setAdapter(listItemAdapter); 
        listView.setOnItemClickListener(this);
        
        if (ApplicationEnvironment.getInstance().getPreferences().getString(Constant.SERVER_ANNOUNCEMENT_LASTEST_NUM, "0").equals(ApplicationEnvironment.getInstance().getPreferences().getString(Constant.SYSTEM_ANNOUNCEMENT_LASTEST_NUM, "0"))){
        	new loadAnnouncementsFromDBTask().execute();
		} else {
			AnnouncementListActivity.this.showDialog(PROGRESS_DIALOG, "正在下载新公告...");
			
			Event event = new Event(null,"downloadNewNotice", null);
			String fskStr = "Get_PsamNo|null#Get_VendorTerID|null";
	        event.setFsk(fskStr);
	        event.setTransfer("999000001");
	        HashMap<String, String> map = new HashMap<String, String>();
	        map.put("noticeVersion", ApplicationEnvironment.getInstance().getPreferences().getString(Constant.SYSTEM_ANNOUNCEMENT_LASTEST_NUM, "0"));
	        event.setStaticActivityDataMap(map);
	        try {
				event.trigger();
			} catch (ViewException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		new loadAnnouncementsFromDBTask().execute();
	}

	@Override
	public void onClick(View arg0) {
		this.finish();
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		try{
			Intent intent = new Intent(this, AnnouncementDetailActivity.class);
			intent.putExtra("announcement", modelList.get(position));
			startActivityForResult(intent, 0);
		} catch(Exception e){
			
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK){
			new loadAnnouncementsFromDBTask().execute();
		}
	}
	
	class loadAnnouncementsFromDBTask extends AsyncTask<Object, Object, Object>{
		@Override
		protected void onPreExecute() {
			AnnouncementListActivity.this.showDialog(PROGRESS_DIALOG, "正在刷新公告数据");
		}
		
		@Override
		protected Object doInBackground(Object... arg0) {
			AnnouncementDBHelper helper = new AnnouncementDBHelper();
			modelList = helper.queryAllAnnouncement();
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			mapList.clear();
			
			if (null != modelList && modelList.size() > 0){
				for (AnnouncementModel model : modelList){
					HashMap<String, String> tempMap = new HashMap<String, String>();
					tempMap.put("title", model.getTitle());
					tempMap.put("content", model.getContent());
					tempMap.put("date", model.getDate());
					mapList.add(tempMap);
				}
				
			} else {
				// 设置空页面
				ImageView emptyView = new ImageView(AnnouncementListActivity.this);
				emptyView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				emptyView.setImageResource(R.drawable.nodata);
				emptyView.setScaleType(ScaleType.CENTER_INSIDE);
				((ViewGroup)listView.getParent()).addView(emptyView);
				listView.setEmptyView(emptyView);
			}
			
			((SimpleAdapter)listView.getAdapter()).notifyDataSetChanged();
			
			AnnouncementListActivity.this.hideDialog(PROGRESS_DIALOG);
		}
	}
	
}
