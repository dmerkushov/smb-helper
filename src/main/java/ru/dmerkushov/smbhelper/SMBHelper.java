/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.dmerkushov.smbhelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import ru.dmerkushov.loghelper.LoggerWrapper;

/**
 *
 * @author Dmitriy Merkushov
 */
public class SMBHelper {

	static LoggerWrapper loggerWrapper = LoggerWrapper.getLoggerWrapper ("SMBHelper");
	static boolean loggerWrapperInitialized = false;

	/**
	 * Copies a file from SFTP to local filesystem byte by byte
	 *
	 * @param remoteSmbFileUrl
	 * @param localFilename
	 * @return
	 * @throws ru.dmerkushov.smbhelper.SMBHelperException
	 */
	public static File copyFileFromSMBToLocal (String remoteSmbFileUrl, String localFilename) throws SMBHelperException {
		if (!loggerWrapperInitialized) {
			initializeLoggerWrapper ();
		}
		Object[] methodParams = {remoteSmbFileUrl, localFilename};
		loggerWrapper.entering (methodParams);

		File result = null;
		
		byte[] fileContents = readBinaryFileFromSMB (remoteSmbFileUrl);

		result = new File (localFilename);

		FileOutputStream resultFos;

		try {
			resultFos = new FileOutputStream (result);
		} catch (FileNotFoundException fnfE) {
			throw new SMBHelperException ("Received a FileNotFoundException when trying to create output file '" + localFilename + "'. It says,\n" + fnfE.getMessage ());
		}

		try {
			resultFos.write (fileContents);
		} catch (IOException ioE) {
			throw new SMBHelperException ("Received an IOException when trying to write to output file '" + localFilename + "'. It says,\n" + ioE.getMessage ());
		}

		try {
			resultFos.close ();
		} catch (IOException ioE) {
			throw new SMBHelperException ("Received an IOException when trying to close output file '" + localFilename + "'. It says,\n" + ioE.getMessage ());
		}

		loggerWrapper.exiting (result);
		return result;
	}

	/**
	 * Reads the contents of a binary file from SFTP. Can read no more than
	 * Integer.MAX_VALUE (currently 2^31-1) bytes
	 *
	 * @param remoteSmbFileUrl
	 * @return
	 * @throws ru.dmerkushov.smbhelper.SMBHelperException
	 */
	public static byte[] readBinaryFileFromSMB (String remoteSmbFileUrl) throws SMBHelperException {
		if (!loggerWrapperInitialized) {
			initializeLoggerWrapper ();
		}
		Object[] methodParams = {remoteSmbFileUrl};
		loggerWrapper.entering (methodParams);

		SmbFile remoteSmbFile;
		try {
			remoteSmbFile = new SmbFile (remoteSmbFileUrl);
		} catch (MalformedURLException ex) {
			throw new SMBHelperException (ex);
		}

		long filesize;
		try {
			filesize = remoteSmbFile.length ();
		} catch (SmbException ex) {
			throw new SMBHelperException (ex);
		}
		int filesizeInt = 0;
		if (filesize > Integer.MAX_VALUE) {
			throw new SMBHelperException ("File '" + remoteSmbFile + "' is too big. Maximum size is " + String.valueOf (Integer.MAX_VALUE) + " bytes");
		} else {
			filesizeInt = (int) filesize;
		}

		loggerWrapper.info ("File length: " + String.valueOf (filesize));

		SmbFileInputStream remoteSmbFis;
		try {
			remoteSmbFis = new SmbFileInputStream (remoteSmbFile);
		} catch (SmbException ex) {
			throw new SMBHelperException (ex);
		} catch (MalformedURLException ex) {
			throw new SMBHelperException (ex);
		} catch (UnknownHostException ex) {
			throw new SMBHelperException (ex);
		}

		byte[] result = new byte[filesizeInt];

		int readResult;
		try {
			readResult = remoteSmbFis.read (result);
		} catch (IOException ex) {
			throw new SMBHelperException (ex);
		}

		loggerWrapper.info ("Successfully read " + String.valueOf (readResult) + " bytes from file '" + remoteSmbFile + "'");

		loggerWrapper.exiting ();	// Not logging the result as it may be huge
		return result;
	}

	/**
	 * Reads the contents of a text file in UTF-8 charset from SFTP
	 *
	 * @param remoteSmbFileUrl
	 * @return
	 * @throws ru.dmerkushov.smbhelper.SMBHelperException
	 */
	public static String readTextFileFromSMB (String remoteSmbFileUrl) throws SMBHelperException {
		if (!loggerWrapperInitialized) {
			initializeLoggerWrapper ();
		}
		Object[] methodParams = {remoteSmbFileUrl};
		loggerWrapper.entering (methodParams);

		String result = readTextFileFromSMB (remoteSmbFileUrl, "UTF-8");

		loggerWrapper.exiting ();	// Not logging the result as it may be huge
		return result;
	}

	/**
	 * Reads the contents of a text file in the specified charset from SMB
	 *
	 * @param remoteSmbFileUrl
	 * @param charset
	 * @return
	 * @throws ru.dmerkushov.smbhelper.SMBHelperException
	 */
	public static String readTextFileFromSMB (String remoteSmbFileUrl, String charset) throws SMBHelperException {
		if (!loggerWrapperInitialized) {
			initializeLoggerWrapper ();
		}
		Object[] methodParams = {remoteSmbFileUrl, charset};
		loggerWrapper.entering (methodParams);

		String result = null;

		byte[] fileBytes = readBinaryFileFromSMB (remoteSmbFileUrl);

		if (Charset.isSupported (charset)) {
			try {
				result = new String (fileBytes, charset);
			} catch (java.io.UnsupportedEncodingException ueE) {
				throw new SMBHelperException ("Received UnsupportedEncodingException when reading file '" + remoteSmbFileUrl + "'. Charset provided is " + charset + ". UnsupportedEncodingException says,\n" + ueE.getMessage ());
			}
		} else {
			throw new SMBHelperException ("Charset " + charset + " is not supported");
		}

		loggerWrapper.exiting ();	// Not logging the result as it may be huge
		return result;
	}

	private static void initializeLoggerWrapper () {
		loggerWrapper.configureByDefaultDailyRolling ("log/SMBHelper_%d_%u.log");
		loggerWrapperInitialized = true;
	}
}
