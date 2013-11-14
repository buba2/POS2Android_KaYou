package com.dhc.pos.activity.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dhc.pos.agent.client.AppDataCenter;
import com.dhc.pos.R;

public class TopInfoView extends LinearLayout{
	
	private TextView phoneNoView = null;
	private TextView dateView = null;
	
	public TopInfoView(Context context) {
		super(context);
		
		init(context);
	}

	public TopInfoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init(context);
	}
	
	private void init(Context context){
		try{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			inflater.inflate(R.layout.topinfoview, this);
			
			phoneNoView = (TextView) this.findViewById(R.id.mobileView);
			dateView = (TextView) this.findViewById(R.id.dateView);
			
			phoneNoView.setText(AppDataCenter.getValue("__PHONENUMWITHLABEL"));
			dateView.setText(AppDataCenter.getValue("__yyyy-MM-dd"));
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
}
