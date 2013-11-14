package com.dhc.pos.activity;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dhc.pos.agent.client.db.AnnouncementDBHelper;
import com.dhc.pos.model.AnnouncementModel;
import com.dhc.pos.R;

public class AnnouncementDetailActivity extends BaseActivity implements OnClickListener {
	
	private Button backButton = null;
	private Button deleteButton = null;
	
	private TextView titleView = null;
	private TextView timeView = null;
	private TextView contentView = null;
	
	private AnnouncementModel model;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.announcement_detail);
		
		backButton = (Button) this.findViewById(R.id.backButton);
		backButton.setOnClickListener(this);
		
		deleteButton = (Button) this.findViewById(R.id.deleteButton);
		deleteButton.setOnClickListener(this);
		
		titleView = (TextView) this.findViewById(R.id.title);
		timeView = (TextView) this.findViewById(R.id.time);
		contentView = (TextView) this.findViewById(R.id.content);
		//contentView.setMovementMethod(ScrollingMovementMethod.getInstance()); // 实现文本可滚动
		
		model = (AnnouncementModel) this.getIntent().getSerializableExtra("announcement");
		titleView.setText(model.getTitle());
		timeView.setText(model.getDate());
		contentView.setText(model.getContent());
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.backButton:
			this.setResult(RESULT_CANCELED);
			this.finish();
			break;
			
		case R.id.deleteButton:
			AlertDialog.Builder builder = new Builder(BaseActivity.getTopActivity());
			builder.setTitle("提示");
			builder.setMessage("确定删除该公告吗，删除后不可恢复？");
			builder.setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					AnnouncementDBHelper helper = new AnnouncementDBHelper();
					if (helper.deleteAnouncement(model.getNumber())){
						AnnouncementDetailActivity.this.setResult(RESULT_OK);
						AnnouncementDetailActivity.this.finish();
					} else {
						AnnouncementDetailActivity.this.setResult(RESULT_CANCELED);
						AnnouncementDetailActivity.this.finish();
						
						Toast.makeText(AnnouncementDetailActivity.this, "删除公告失败", Toast.LENGTH_SHORT).show();
					}
				}
			});
			builder.setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			builder.show();
			break;
		}
	}
	
	

}
