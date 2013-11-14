package com.dhc.pos.model;

import java.util.ArrayList;

public class TransferModel {

	private boolean shouldMac;
	
	private ArrayList<FieldModel> fieldList;
	
	public TransferModel(){
		fieldList = new ArrayList<FieldModel>();
	}

	public boolean shouldMac() {
		return shouldMac;
	}

	public void setShouldMac(String shouldMac) {
		if ("true".equalsIgnoreCase(shouldMac))
			this.shouldMac = true;
		else
			this.shouldMac = false;
	}

	public ArrayList<FieldModel> getFieldList() {
		return fieldList;
	}

	public void addField(FieldModel field) {
		this.fieldList.add(field);
	}
	
}
