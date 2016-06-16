package com.hsjry.mavenplugin.commons;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class SecurertUtil {
	private static final String DEFAULTENCODING = "UTF-8";
	private SecurertUtil(){}
	
	/**
	 * ��������
	 * @param data
	 * @param key
	 * @return
	 */
	public static String encryptData(String data,String key){
		try {
			byte[] dataBytes = data.getBytes(DEFAULTENCODING);
			byte[] decryptData = SecurertUtil.decryptOrEncryptData(dataBytes, 0, dataBytes.length, key, Cipher.ENCRYPT_MODE);
			sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
			String encodeBuffer = encoder.encodeBuffer(decryptData);
			return encodeBuffer;
		} catch (Exception e) {
			throw new RuntimeException("����ʧ��",e);
		}
	}
	
	/**
	 * ��������
	 * @param sources
	 * @param key
	 * @return
	 */
	public static String decryPwd(String sources,String key){
		try {
			sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
			byte[] decodeBuffer = decoder.decodeBuffer(sources);
			byte[] decryptData = SecurertUtil.decryptOrEncryptData(decodeBuffer, 0, decodeBuffer.length, key, Cipher.DECRYPT_MODE);
			return new String(decryptData,DEFAULTENCODING);
		} catch (Exception e) {
			throw new RuntimeException("",e);
		}
	}
	
	
	/**
	 * ע�ⲻҪ�Լ��ܺ���ֽ�����ת���ַ���(�����ǳ�Ҫת��BASE64����������ת�����ַ���)
	 * @param bys ���� 
	 * @param model Cipher.DECRYPT_MODE����ģʽ    Cipher.ENCRYPT_MODE Cipher.ENCRYPT_MODE����ģʽ
	 * @return byte[]
	 * @throws Exception
	 */
	private static byte[]  decryptOrEncryptData(byte[] bys,int start,int length,String key,int model)throws Exception{
	     DESKeySpec desKeySpec = new DESKeySpec(key.getBytes(DEFAULTENCODING));
        SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
        SecretKey desSecretKey = factory.generateSecret(desKeySpec);
        Cipher encryptDesCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        encryptDesCipher.init(model, desSecretKey);
        // ִ�м��ܲ���
        byte[]  cryptoText = encryptDesCipher.doFinal(bys,start,length);
        return cryptoText;
	}

}
