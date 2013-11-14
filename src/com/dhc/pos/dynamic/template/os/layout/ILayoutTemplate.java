package com.dhc.pos.dynamic.template.os.layout;

import java.util.Vector;

import com.dhc.pos.dynamic.component.Component;
import com.dhc.pos.dynamic.component.ViewException;

import android.view.View;

public interface ILayoutTemplate {
	
	public View rewind(Vector<Component> components) throws ViewException;

}
