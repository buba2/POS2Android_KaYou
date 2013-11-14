package com.dhc.pos.dynamic.template.os;

import android.view.View;

import com.dhc.pos.dynamic.parse.ParseView;


/**
 * 框架整体背景样式
 * @author Xiaoping Dong
 */

public class BodyTemplate extends CSSTemplate {
	private int pageBgImage;
	
	public int getPageBgImage() {
		return pageBgImage;
	}

	public void setPageBgImage(int pageBgImage) {
		this.pageBgImage = pageBgImage;
	}

	public BodyTemplate(String id, String name) {
		super(id, name);
		this.setType(ParseView.TEMPLATE_TYPE_STRUCT);
	}
	
	@Override
	public View rewind(View structComponent) {
		// TODO
		structComponent.setBackgroundResource(this.pageBgImage);
		return structComponent;
	}

}
