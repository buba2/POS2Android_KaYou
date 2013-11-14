/**
 * 
 */
package com.dhc.pos.dynamic.component.os.frame;

import java.util.Vector;

import android.view.View;

import com.dhc.pos.dynamic.component.Component;
import com.dhc.pos.dynamic.component.ViewException;
import com.dhc.pos.dynamic.core.ViewPage;

/**
 * @author STH
 *
 */
public abstract class FrameComponent extends Component {
	public FrameComponent(ViewPage viewPage) {
		this.viewPage = viewPage;
	}
	public final View toFrame(Vector<Component> components) throws ViewException {
		return this.initOSParams(this.toOSFrame(components));
	}
	
	protected abstract View toOSFrame(Vector<Component> components) throws ViewException;
}
