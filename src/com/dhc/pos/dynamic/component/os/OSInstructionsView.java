package com.dhc.pos.dynamic.component.os;

import com.dhc.pos.activity.view.InstructionsForUseView;
import com.dhc.pos.dynamic.component.Component;
import com.dhc.pos.dynamic.component.ViewException;
import com.dhc.pos.dynamic.core.ViewPage;

public class OSInstructionsView extends StructComponent {
	
	private String instructionId;
	
	public OSInstructionsView(ViewPage viewPage) {
		super(viewPage);
	}

	@Override
	public InstructionsForUseView toOSComponent() throws ViewException {
		InstructionsForUseView view = new InstructionsForUseView(this.getContext());
		view.setTag(this.getId());
		view.getInstructionsText().setText(view.getInstructionContent(instructionId));
		return view;
	}

	@Override
	protected Component construction(ViewPage viewPage) {
		return new OSInstructionsView(viewPage);
	}

	public void setInstructionId(String instructionId) {
		this.instructionId = instructionId;
	}
	
}
