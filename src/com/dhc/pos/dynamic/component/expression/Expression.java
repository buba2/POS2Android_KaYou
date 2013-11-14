/**
 * 
 */
package com.dhc.pos.dynamic.component.expression;

import com.dhc.pos.dynamic.component.Component;
import com.dhc.pos.dynamic.component.ViewException;
import com.dhc.pos.dynamic.core.ViewPage;

/**
 * 表达式标签，可实现逻辑表达式及结构化表达式
 * @author DongXiaoping
 *
 */
public abstract class Expression extends Component {
	
	private String conditionRegex;
	
	public Expression(ViewPage viewPage) {
		this.viewPage = viewPage;
	}
	public String getConditionRegex() {
		return conditionRegex;
	}
	public void setConditionRegex(String conditionRegex) {
		this.conditionRegex = conditionRegex;
	}
	
	@Override
	protected void copyParams(Component src, Component des) {
		super.copyParams(src, des);
		((Expression) des).setConditionRegex(((Expression)des).conditionRegex);
	}
	
	protected abstract void parseCondition() throws ViewException;
	
	
}
