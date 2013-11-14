/**
 * 
 */
package com.dhc.pos.dynamic.component.expression;

import com.dhc.pos.dynamic.component.Component;
import com.dhc.pos.dynamic.core.ViewPage;

/**
 * @author Administrator
 *
 */
public class If extends Logic {

	public If(ViewPage viewPage, String conditionRegex) {
		super(viewPage, conditionRegex);
	}

	/* (non-Javadoc)
	 * @see com.zhc.mbank.dynamic.component.Component#construction(com.zhc.mbank.dynamic.core.ViewPage)
	 */
	@Override
	protected Component construction(ViewPage viewPage) {
		return new If(viewPage, this.getConditionRegex());
	}
}
