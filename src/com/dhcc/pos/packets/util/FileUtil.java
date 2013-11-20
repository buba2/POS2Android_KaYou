package com.dhcc.pos.packets.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;


public class FileUtil {
	
	
	/**
	 * 不可追加内容（每执行一次就会替换一次文件内容）
	 * 
	 * @param path
	 *            请求报文写入路径
	 * @param fileName
	 *            文件名
	 * @param content
	 *            内容（写入内容）
	 * @param append
	 * 			  追加（
	 * 					true：	在该文件中追加内容，不会替换;
	 * 					false：	不追加内容，每执行一次该文件都会替换一次全新内容;
	 * 					）        
	 * @return
	 */
	public static boolean writeFile(String path, String fileName, String content, boolean append) {
		System.out.println("\t############写文件#############");
		boolean flag = false;
		StringBuffer buf = new StringBuffer();
		buf.append(content.trim());

		String filePath = path;
		File dir = new File(path);
		if (!dir.exists())
			dir.mkdir();

		filePath = filePath + File.separator + fileName;
		FileWriter fw = null;
		try {
			fw = new FileWriter(new File(filePath), append);
			fw.write(buf.toString());
			flag = true;
		} catch (IOException e) {
			e.printStackTrace();
			flag = false;
		} finally {
			try {
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return flag;
	}

	/**
	 * BufferedReader
	 * 
	 * @param path
	 *            要读取文件所在的目录路径
	 * @param fileName
	 *            要读取文件名称
	 * @param type 读取函数类型 1：  BufferedReader 2、3、         
	 * @return
	 */
	public static String readerFile(String path, String fileName,int type) {
		System.out.println("\t############读文件#############");
		String result = null;
		String data = null;
		BufferedReader br = null;
		FileReader fr = null;
		StringBuffer sb = null;
		
		path += File.separator + fileName;

		File file = new File(path);
		
		if(!file.canRead()){
			return null;
		}
	
		switch (type) {
		case 1:
			sb = new StringBuffer();
			try {
				
				br = new BufferedReader(new InputStreamReader(new FileInputStream(
						file)));
				if (br != null) {
					while ((data = br.readLine())!= null) {
						sb.append(data);
					}
				}
				result =  sb.toString();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		case 2:
			int num = 0;
			try {
				
				fr = new FileReader(file);
				while((num = fr.read())!=-1){
					System.out.println("@@:" + num);
				}
				result = sb.toString();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 3:
			break;
		default:
			break;
		}
		return result;
	}
}
