package com.dhcc.pos.packets.util;

/**=========================================================
 * 版权：
 * 文件： 
 * 所含类: PMAC.java
 * 修改记录：
 * 日期                  作者           内容
 * =========================================================
 * Nov 7, 2013          李兴魁         创建文件，实现基本功能
 */


import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import com.dhcc.pos.packets.util.ByteOrHexString;
import com.dhcc.pos.packets.util.ConvertUtil;
import com.dhcc.pos.packets.util.FileUtil;

/*
 mac流程：
 1：输入字符串SRC和KEY值
 2：将字符串SRC分组，每组8个字节，不足时进行填充0X00.
 3：遍历分组进行异或运算。
 4：异或运算完成后对结果进行HEX转换。
 5：将HEX转换的结果前8位进行一次DES操作。
 6：DES操作的结果HEX转换结果的后8位进行xor（异或）运算。
 7：得到的结果再进行一次DES操作。
 8：将DES操作的结果转换成HEX。
 9：取HEX转换结果的前8位作为MAC值
 */
public class PMAC {
	
	public static void main(String[] args) throws Exception {
		// Key key =pMac.generateKey();
		// key = new
		// javax.crypto.spec.SecretKeySpec("F415DA6DC7E507491973A72C7649EF94".getBytes(),"3DES");
//		macFull();
		testMac();
	}
	
	/**计算mac	
	 * 1、从主密钥解析密文的mac秘钥到 
	 * 2、以mac秘钥计算mac数据 得到8个字节的mac值
	 * @throws Exception
	 */
	public static void testMac(){
		/**主秘钥*/
		Key masterKey;
		/**主密钥串*/
		String _masterKey = "F415DA6DC7E507491973A72C7649EF94";
		/**密文工作密钥*/
		String makey = "9986E1550F53AB2D0FC9AB73B03773DE7A502096426C5EBE02544AAE000000000000000051F4E614";
		
		try {
			masterKey = getKey(ByteOrHexString.hexStringToBytes(_masterKey));
		
			/*1、从文件中拿到工作秘钥*/
			makey = FileUtil.readerFile("/Users/xs/Documents/Project/Workspaces_dhcc.pos.TDW/com.dhcc.tdw.iso8583/", "f62", 1);
						
			
			macFull(masterKey, makey, "0210703A008102C08411164392257501725638000000000000000004980097000000000000000008000000004130313935333030323431393533303034333031303130303131353600083032313031353643000822000001".getBytes());
//			macFull(_masterKey, makey, "0210703A008102C08411164392257501725638000000000000000004980097000000000000000008000000004130313935333030323431393533303034333031303130303131353600083032313031353643000822000001".getBytes());
//			macFull(ByteOrHexString.hexStringToBytes(_masterKey), makey, "0210703A008102C08411164392257501725638000000000000000004980097000000000000000008000000004130313935333030323431393533303034333031303130303131353600083032313031353643000822000001".getBytes());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**根据 主秘钥 、工作秘钥、源数据 得到mac
	 * @param masterKey 终端主秘钥
	 * @param makey 密文工作密钥(仅限80位长)
	 * @param data 加密数据
	 * @return 8个字节mac值
	 * @throws Exception
	 */
	public static byte[] macFull(Key masterKey,String makey, byte[] data) throws Exception{
		/**主密钥，mac密钥*/
		Key macKey;

		/**密文mackey*/
		String cipherMacKey = null;
		
		/**明文mackey*/
		byte[] plainMacKey = null;
		
		/**mac 校验值*/
		String checkValue = null;
		
		PMAC pMac = null;
		
		byte[] macValue = null;
		
		/*明文mackey对8个字节（16个长度）的0做单倍长密钥算法*/
		byte[] temp = null;
		
		
		System.out.println("【masterKey】" + ConvertUtil.trace(masterKey.getEncoded()) );
		System.out.println("【填充方式】==》\t" + masterKey.getAlgorithm());
		System.out.println("【密钥编码格式的名称】==》\t" + masterKey.getFormat());
		
		System.out.println("【工作秘钥】==》\t" + makey);
		
		/*2、从工作秘钥中截出mackey的密文（8个字节16个长度）*/
		cipherMacKey =  makey.substring(40, 56);
		System.out.println("【密文mackey】==》" + cipherMacKey);
		
		/*实例化PMAC工具类 本类中不需要*/
		pMac = new PMAC();
		
		/*解密mackey*/
		plainMacKey = pMac.desDecrypt(masterKey, ByteOrHexString.hexStringToBytes(cipherMacKey));
	
		System.out.println("【解密出的明文mackey】" + ConvertUtil.trace(plainMacKey)) ;
		/*拿到mackey校验值*/
		checkValue =  makey.substring(72, 80);
	
		try {
			temp = pMac.des(ByteOrHexString.hexStringToBytes("0000000000000000"), pMac.getKey(plainMacKey));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("【mackey对0加密后的值】\t" + ConvertUtil.trace(temp));
		System.out.println("【checkValue】==》\t" +checkValue);
		
		/**校验mackey
		 * mackey对8字节0加密后的hex值前8为和checkValue作对比
		 * */
		if(ByteOrHexString.bytes2HexString(temp).substring(0, 8).equals(checkValue)){
			System.out.println("#######解密mackey验证通过###########");
		}else{
			System.out.println("#######解密mackey验证失败###########");
			
		}

		try {
			
			macKey = PMAC.getKey(plainMacKey);
			
			macValue = pMac.xorDataAndMac(data, macKey);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}								
		return macValue;
	}
	/**根据 主秘钥 、工作秘钥、源数据 得到mac
	 * @param masterKey 终端主秘钥
	 * @param makey 密文工作密钥(仅限80位长)
	 * @param data 加密数据
	 * @return 8个字节mac值
	 * @throws Exception
	 */
	public static byte[] macFull(String masterKeySrc, String makey, byte[] data) throws Exception{
		/**主密钥，mac密钥*/
		Key masterKey ;

		/**8字节mac值*/
		byte[] macValue = null;
		
		/**16进制转换为字节masterKey*/
		byte[] masterKeyByte = ByteOrHexString.hexStringToBytes(masterKeySrc);
		
		masterKey = getKey(masterKeyByte);
		
		macValue = macFull(masterKey, makey, data);						
		return macValue;
	}
	
	/**根据 主秘钥 、工作秘钥、源数据 得到mac
	 * @param masterKeyByte 终端主秘钥
	 * @param makey 密文工作密钥(仅限80位长)
	 * @param data 加密数据
	 * @return 8个字节mac值
	 * @throws Exception
	 */
	public static byte[] macFull(byte[] masterKeyByte, String makey, byte[] data) throws Exception{
		/**主密钥*/
		Key masterKey ;
		
		/**8字节mac值*/
		byte[] macValue = null;
		
		masterKey = getKey(masterKeyByte);
		
		macValue = macFull(masterKey, makey, data);

		return macValue;
	}

	/**根据源数组 按每8个字节一组进行遍历异或 不满8字节补零 得到异或后的最后8个字节数组
	 * @param data 需要进行遍历分组异或的数据
	 * @return 异或之后的8字节数组
	 */
	public byte[] xorData(byte[] data){
		System.out.println("################### 遍历异或处理 ###################");
		System.out.println("接收到的数据:" + ConvertUtil.trace(data));
		
		// 填充
		// 1：数据源
		// byte[] oSrc = src.getBytes();
		byte[] oSrc = data;

		int oLength = oSrc.length;

		// 2：目标数据源，用之后一系列计算，即用于填充：不需填充时直COPY否则需填充0X00
		byte[] nSrc;
		if (oLength % 8 == 0) {
			nSrc = new byte[oLength];
			System.arraycopy(oSrc, 0, nSrc, 0, oLength);
		} else {
			nSrc = new byte[(oLength / 8 + 1) * 8];
			System.arraycopy(oSrc, 0, nSrc, 0, oLength);
			for (int i = oLength; i < nSrc.length; i++) {
				nSrc[i] = 0X00;
			}
		}
		System.out.println("自动填充（0X00）后的数据:" + ConvertUtil.trace(nSrc));

		// 3：遍历分组进行xor（异或）运算。
		int reapt = nSrc.length / 8;
		byte[] b1 = new byte[8];
		byte[] b2 = new byte[8];
		// 异或运算的结果集
		byte[] temp = new byte[8];

		if (reapt > 1) {
			System.arraycopy(nSrc, 0, b1, 0, 8);
			System.out.println("第" + 1 + "个 B1【" + ByteOrHexString.bytes2HexString(b1) + "】" );
			System.arraycopy(nSrc, 8, b2, 0, 8);
			System.out.println("第" + 2 + "个 B2【" + ByteOrHexString.bytes2HexString(b2) + "】" );
			temp = xor(b1, b2);

			int j = 3;
			for (int i = 2; i < reapt; i++) {
				System.arraycopy(nSrc, i * 8, b1, 0, 8);
				System.out.println("第" + j + "个 B1【" + ByteOrHexString.bytes2HexString(b1) + "】" );
				j = j++;
				temp = xor(b1, temp);
			}

		}else if(reapt == 1){
			temp = nSrc;
		}else {
			System.out.println("数组长度太短，少于8个字节");
			return null;
		}

		System.out.println("【异或之后的8字节数组】:" + ConvertUtil.trace(temp));

		return temp;
		
	}
	

	/**异或并且计算mac值
	 * @param data 需要进行mac计算的源数据
	 * @param macKey 
	 * @return macValue 8字节mac值
	 */
	public byte[] xorDataAndMac(byte[] data, Key macKey){
		System.out.println("################### MAC计算 ###################");
		System.out.println("接收到的数据:" + ConvertUtil.trace(data));
		/*异或运算的结果集*/
		byte[] xorAfterData = new byte[8];
		
		byte[] macValue = null;
		
		xorAfterData = xorData(data);
		
		try {
			macValue = xorAfterDataToMac(xorAfterData, macKey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return macValue;
		
	}

	

	/**
	 * 二个八字节进行XOR(异或)运算
	 * 
	 * @param b1
	 * @param b2
	 * @return 八字节数组
	 */
	public byte[] xor(byte[] b1, byte[] b2) {
		byte[] result = new byte[8];
		for (int i = 0; i < 8; i++) {
			result[i] = (byte) (b1[i] ^ b2[i]);
		}
		return result;
	}

	/**
	 * 将字节转换成HEX形式
	 * 
	 * @param bArray
	 * @return HEX字符串
	 */
	public String byteToHex(byte[] bArray) {
		String result = "";
		byte b;
		for (int i = 0; i < bArray.length; i++) {
			b = bArray[i];
			result += Integer.toHexString((0x000000ff & b) | 0xffffff00)
					.substring(6);
		}
		return result;
	}

	/**
	 * 密钥
	 * 
	 * @param src
	 * @param key
	 * @return
	 */
	public byte[] des(byte[] src, Key key) {
		byte[] plainText = src;
		byte[] cipherText = null;
		Cipher cipher;
		try {
			// 很关键的一个地方API或算法本身不对数据进行处理，加密数据由加密双方约定填补算法。
			cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			cipherText = cipher.doFinal(plainText);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cipherText;
	}

	/**
	 * 解密DES
	 * @param key 明文密钥
	 * @param cipherText 要解密数据（被加密数据）
	 * @return	解密后的明文字节数组
	 */
	public byte[] desDecrypt(Key key, byte[] cipherText) {
		byte[] plainText = null;
		Cipher cipher;
		// 很关键的一个地方API或算法本身不对数据进行处理，加密数据由加密双方约定填补算法。
			try {
				cipher = Cipher.getInstance("DES/ECB/NoPadding");
				cipher.init(Cipher.DECRYPT_MODE, key);
				
				plainText = cipher.doFinal(cipherText);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		return plainText;
	}
	
	/**
	 * 随机产生一个KEY generating DES key
	 * 
	 * @return
	 */
	public Key generateKey() {
		KeyGenerator keyGen;
		Key key = null;
		try {
			keyGen = KeyGenerator.getInstance("DES");
			keyGen.init(56);
			key = keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return key;
	}

	/**
	 * 从指定字符串生成密钥，密钥所需的字节数组长度为8位 不足8位时后面补0，超出8位只取前8位
	 * 
	 * @param arrBTmp
	 *            构成该字符串的字节数组
	 * @return 生成的密钥
	 * @throws java.lang.Exception
	 */
	public static Key getKey(byte[] arrBTmp) throws Exception {
		// 创建一个空的8位字节数组（默认值为0）
		byte[] arrB = new byte[8];
		// 将原始字节数组转换为8位
		for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {
			arrB[i] = arrBTmp[i];
		}
		// 生成密钥
		Key key = new javax.crypto.spec.SecretKeySpec(arrB, "DES");

		return key;
	}

	public static Key getKey2(byte[] arrBTmp) throws Exception {
		// 生成密钥
		Key key = new javax.crypto.spec.SecretKeySpec(arrBTmp, "DES");

		return key;
	}
	
	/**
	 * 根据异或后的值计算mac
	 * 
	 * @param xorAfterData
	 *            异或之后的8字节数组
	 * @param key
	 *            加密秘钥
	 * @return 返回MAC值（8字节）
	 * @throws Exception 
	 */
	public byte[] xorAfterDataToMac(byte[] xorAfterData, Key maKey) throws Exception {
		byte[] b1 = new byte[8];
		byte[] b2 = new byte[8];

		System.out.println("【异或之后的8字节数组】:" + ConvertUtil.trace(xorAfterData));
//		System.out.println("【异或之后的8字节数组】:" + ByteOrHexString.bytesToHexString(xorAfterData));
		
		// 4：将异或运算后的最后8个字节结果转换成HEX
//		String strHex = byteToHex(xorAfterData);
		
		String strHex = ByteOrHexString.bytes2HexString(xorAfterData);
		System.out.println("异或后的最后8个字节转换为HEX ==》【" + strHex + "】");

		
		System.out.println("【异或后的数据HEX之后的值ASCII码显示为】:" + ConvertUtil.trace(strHex.getBytes()));
		
		
		// 5：取前8个字节用MAK加密(进行DES)
		System.arraycopy(strHex.getBytes(), 0, b1, 0, 8);
		System.out.println("【取前8字节进行MAK加密的数据】" + ConvertUtil.trace(b1));
//		System.out.println("【取前8字节进行MAK加密的数据】" + ByteOrHexString.bytes2HexString(b1));
		b1 = des(b1, maKey);
		System.out.println("【进行DES后的值】" + ByteOrHexString.bytes2HexString(b1));
		
		
		// 6：前8个字节进行DES的结果值与后8个字节（第4部分中得）异或运算
		System.arraycopy(strHex.getBytes(), 8, b2, 0, 8);
		System.out.println("【取后8字节的数据】" + ConvertUtil.trace(b2));
//		System.out.println("【取后8字节的数据】" + ByteOrHexString.bytes2HexString(b2));
		xorAfterData = xor(b1, b2);
		System.out.println("【前8个字节进行DES的结果值与后8个字节（第4部分中得）异或运算值】" + ConvertUtil.trace(xorAfterData));
//		System.out.println("【前8个字节进行DES的结果值与后8个字节（第4部分中得）异或运算值】" + ByteOrHexString.bytes2HexString(xorAfterData));
		
		
		// 7：用异或的结果,再一次用MAK加密（单倍长密钥算法运算）（进行DES运算）
		xorAfterData = des(xorAfterData, maKey);
		System.out.println("【异或的结果再次MAK加密后的数据】" + ConvertUtil.trace(xorAfterData));
//		System.out.println("【异或的结果再次MAK加密后的数据】" + ByteOrHexString.bytes2HexString(xorAfterData));
		
		// 8：将DES结果再一次转换成HEX
		strHex = ByteOrHexString.bytes2HexString(xorAfterData);
		System.out.println("DES结果再次转换HEX【" + strHex + "】");

		// 9：MAC (取前8个字节作为MAC值。)
		byte[] macValue =  strHex.substring(0, 8).getBytes();
		
		System.out.println("【MAC】(HEX)" + ByteOrHexString.bytes2HexString(macValue));
		System.out.println("【MAC】(String)" + new String(macValue));
		
		return macValue;
	}
}
