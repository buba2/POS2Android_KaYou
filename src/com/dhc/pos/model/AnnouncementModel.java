package com.dhc.pos.model;

import java.io.Serializable;
import java.util.HashMap;

public class AnnouncementModel implements Serializable{

	private static final long serialVersionUID = -8307674336517627131L;
	
	private String number = "";
	private String title = "";
	private String date = "";
	private String content = "";
	
	public AnnouncementModel(){
		
	}
	
	public AnnouncementModel(String number, String title, String date, String content) {
		this.number = number;
		this.title = title;
		this.date = date;
		this.content = content;
	}
	
	public AnnouncementModel (HashMap<String, String> map){
		if (map.containsKey("id_notice")){
			this.number = map.get("id_notice");
		} 
		if (map.containsKey("title_notice")){
			this.title = map.get("title_notice");
		}
		if (map.containsKey("content_notice")){
			this.content = map.get("content_notice");
		}
		if (map.containsKey("effective_date")){
			this.date = map.get("effective_date");
		}
	}
	
	public String getNumber() {
		return number;
	}
	
	public void setNumber(String number) {
		this.number = number;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
}
