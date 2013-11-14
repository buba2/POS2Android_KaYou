package com.dhc.pos.agent.client.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dhc.pos.model.AnnouncementModel;


public class AnnouncementDBHelper extends BaseDBHelper {
	
	// 保存一条公告信息
	public boolean insertAnnouncement(AnnouncementModel model){
		SQLiteDatabase db = this.getWritableDatabase();
		try{
			ContentValues values = new ContentValues();
			values.put("number", model.getNumber());
			values.put("title", model.getTitle());
			values.put("date", model.getDate());
			values.put("content", model.getContent());
			
			long count = db.insert(ANNOUNCEMENT_TABLE, null, values);
			return count!=-1;
			
		} catch(Exception e){
			e.printStackTrace();
			return false;
			
		} finally{
			db.close();
		}
 	}
	
	// 查询所有公告信息
	public ArrayList<AnnouncementModel> queryAllAnnouncement(){
		ArrayList<AnnouncementModel> list = new ArrayList<AnnouncementModel>();
		
		SQLiteDatabase db = this.getReadableDatabase();
		try{
			String sql = "SELECT number, title, date, content FROM " + ANNOUNCEMENT_TABLE;
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()){
				AnnouncementModel model = new AnnouncementModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
				list.add(model);
			}
			cursor.close();
			
		} catch(Exception e){
			e.printStackTrace();
			
		} finally{
			db.close();
		}
		
		return list;
	}
	
	// 删除一条公告
	public boolean deleteAnouncement(String number){
		SQLiteDatabase db = this.getWritableDatabase();
		try{
			int rows = db.delete(ANNOUNCEMENT_TABLE, "number = ?", new String[]{number});
			if (rows == 0){
				return false;
			} else {
				return true;
			}
		} catch(Exception e){
			e.printStackTrace();
			return false;
		} finally{
			db.close();
		}
	}
	

}
