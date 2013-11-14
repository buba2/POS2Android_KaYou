package com.dhc.pos.util;

public class XORUtil {

	public static byte[] xorAfterData(byte[] data) {
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

		// 3：遍历分组进行xor（异或）运算。
		int reapt = nSrc.length / 8;
		byte[] b1 = new byte[8];
		byte[] b2 = new byte[8];
		// 异或运算的结果集
		byte[] temp = new byte[8];

		if (reapt > 1) {
			System.arraycopy(nSrc, 0, b1, 0, 8);
			System.arraycopy(nSrc, 8, b2, 0, 8);
			temp = xor(b1, b2);

			int j = 3;
			for (int i = 2; i < reapt; i++) {
				System.arraycopy(nSrc, i * 8, b1, 0, 8);
				j = j++;
				temp = xor(b1, temp);
			}

		} else if (reapt == 1) {
			temp = nSrc;
		} else {
			System.out.println("数组长度太短，少于8个字节");
			return null;
		}

		return temp;

	}

	/**
	 * 二个八字节进行XOR(异或)运算
	 * 
	 * @param b1
	 * @param b2
	 * @return 八字节数组
	 */
	private static byte[] xor(byte[] b1, byte[] b2) {
		byte[] result = new byte[8];
		for (int i = 0; i < 8; i++) {
			result[i] = (byte) (b1[i] ^ b2[i]);
		}
		return result;
	}

}
