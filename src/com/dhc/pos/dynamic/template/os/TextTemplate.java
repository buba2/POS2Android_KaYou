/**
 * 
 */
package com.dhc.pos.dynamic.template.os;

import android.view.View;

import com.dhc.pos.activity.view.TextWithLabelView;
import com.dhc.pos.dynamic.parse.ParseView;

/**
 * 文本框模板样式
 */
public class TextTemplate extends CSSTemplate {
	public TextTemplate(String id, String name) {
		super(id, name);
		this.setType(ParseView.TEMPLATE_TYPE_TEXT);
	}
	@Override
	public TextWithLabelView rewind(View structComponent) {
		if (null != this.getColor()) {
			((TextWithLabelView)structComponent).getEditText().setTextColor(this.getColor());
		}
		if (null != this.getSize()) {
			((TextWithLabelView)structComponent).getEditText().setTextSize(this.getSize());
		}
		if(null != this.getGravity()){
			((TextWithLabelView)structComponent).setGravity(this.getGravity());
		}
		if(null != this.getBgImage()){
			int resourceId = structComponent.getContext().getResources().getIdentifier(this.getBgImage(), "drawable", structComponent.getContext().getPackageName());
			((TextWithLabelView)structComponent).setBackgroundResource(resourceId);
		}
		
		return ((TextWithLabelView)structComponent);
	}
	
}
