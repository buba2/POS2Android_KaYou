package com.dhc.pos.dynamic.model;

import java.util.ArrayList;
import java.util.List;

import com.dhc.pos.dynamic.component.Component;
import com.dhc.pos.dynamic.core.Event;

public final class ViewGroupBean {
	private List<Component> components;
	private Event event;
	public ViewGroupBean() {
		this.components = new ArrayList<Component>();
	}
	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public void addAComponent(Component component) {
		this.components.add(component);
	}
	public List<Component> getComponents() {
		return this.components;
	}
}
