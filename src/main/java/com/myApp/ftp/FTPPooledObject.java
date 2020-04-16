package com.myApp.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xuemc
 *  FTPPooledObject封装FTPClient对象,相当于DefaultPooledObject和ApacheFtpUtil功能的合集
 */
public class FTPPooledObject extends DefaultPooledObject<FTPClient> {

	public FTPPooledObject(FTPClient object) {
		super(object);
	}

	private static Logger log = LoggerFactory.getLogger(FTPPooledObject.class);

	public static final int BINARY_FILE_TYPE = FTP.BINARY_FILE_TYPE;
	public static final int ASCII_FILE_TYPE = FTP.ASCII_FILE_TYPE;

	public void setPassiveNatWorkaround(boolean _passiveMode) {
		super.getObject().setPassiveNatWorkaround(_passiveMode);
	}

	public void enterLocalPassiveMode() {
		super.getObject().enterLocalPassiveMode();
	}

	public void enterLocalActiveMode() {
		super.getObject().enterLocalActiveMode();
	}

	// FTP.BINARY_FILE_TYPE | FTP.ASCII_FILE_TYPE
	// Set transform type
	public void setFileType(int fileType) throws IOException {
		super.getObject().setFileType(fileType);

	}

	public boolean rename(String oldName, String newName) throws IOException {
		return super.getObject().rename(oldName, newName);
	}

	// =======================================================================
	// == About directory =====
	// The following method using relative path better.
	// =======================================================================

	/**
	 * 跳转至指定目录(支持多层)(支持绝对路径、相对路径)
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public boolean changeDirectory(String path) throws IOException {
		return super.getObject().changeWorkingDirectory(path);
	}

	/**
	 * 创建目录(不支持一次创建多层)
	 */
	public boolean createDirectory(String pathName) throws IOException {
		pathName = new String(pathName.getBytes("UTF-8"), "iso-8859-1");

		boolean flag = true;
		try {
			flag = super.getObject().makeDirectory(pathName);
			if (flag) {
				log.info("make Directory " + pathName + " succeed");
			} else {
				log.info("make Directory " + pathName + " false");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * 创建目录(支持多层)(支持绝对路径、相对路径)
	 * 
	 * @param multiDir
	 *            例："/11/22/33"、"11/22/33"
	 * @return
	 * @throws IOException
	 */
	public boolean createMultiDir(String multiDir) throws IOException {
		String presentWorkingDir = super.getObject().printWorkingDirectory();
		boolean isAbsolutePath = multiDir.startsWith("/");
		String[] dirList = multiDir.split("/");

		if (isAbsolutePath) // 参数是绝对路径
			changeDirectory("/");

		for (int i = 0; i < dirList.length; i++) {
			if (!existDirectory("", dirList[i]))
				createDirectory(dirList[i]);
			changeDirectory(dirList[i]);
		}

		changeDirectory(presentWorkingDir);
		boolean success = changeDirectory(multiDir);
		changeDirectory(presentWorkingDir);

		return success;
	}

	public boolean removeDirectory(String path) throws IOException {
		return super.getObject().removeDirectory(path);
	}

	// delete all subDirectory and files.
	public boolean removeDirectory(String path, boolean isAll) throws IOException {

		if (!isAll) {
			return removeDirectory(path);
		}

		FTPFile[] ftpFileArr = super.getObject().listFiles(path);
		if (ftpFileArr == null || ftpFileArr.length == 0) {
			return removeDirectory(path);
		}
		//
		for (FTPFile ftpFile : ftpFileArr) {
			String name = ftpFile.getName();
			if (ftpFile.isDirectory()) {
				log.info("* [sD]Delete subPath [" + path + "/" + name + "]");
				removeDirectory(path + "/" + name, true);
			} else if (ftpFile.isFile()) {
				log.info("* [sF]Delete file [" + path + "/" + name + "]");
				deleteFile(path + "/" + name);
			} else if (ftpFile.isSymbolicLink()) {

			} else if (ftpFile.isUnknown()) {

			}
		}
		return super.getObject().removeDirectory(path);
	}

	// Check the path is exist; exist return true, else false.
	/**
	 * findInPath目录下是否存在path目录
	 * 
	 * @param findInPath
	 *            目录名 例："/" 、 "/home" 、""
	 * @param findInPath
	 *            目录名 例："test"
	 * @return
	 * @throws IOException
	 */
	public boolean existDirectory(String findInPath, String path) throws IOException {
		boolean flag = false;
		FTPFile[] ftpFileArr = super.getObject().listFiles(findInPath);
		for (FTPFile ftpFile : ftpFileArr) {
			if (ftpFile.isDirectory() && ftpFile.getName().equalsIgnoreCase(path)) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	// =======================================================================
	// == About file =====
	// Download and Upload file using
	// ftpUtil.setFileType(FtpUtil.BINARY_FILE_TYPE) better!
	// =======================================================================

	// #1. list & delete operation
	// Not contains directory
	public List<String> getFileList(String path) throws IOException {
		// listFiles return contains directory and file, it's FTPFile instance
		// listNames() contains directory, so using following to filer
		// directory.
		// String[] fileNameArr = ftpClient.listNames(path);
		FTPFile[] ftpFiles = super.getObject().listFiles(path);

		List<String> retList = new ArrayList<String>();
		if (ftpFiles == null || ftpFiles.length == 0) {
			return retList;
		}
		for (FTPFile ftpFile : ftpFiles) {
			if (ftpFile.isFile()) {
				retList.add(ftpFile.getName());
			}
		}
		return retList;
	}

	public boolean deleteFile(String pathName) throws IOException {
		return super.getObject().deleteFile(pathName);
	}

	// #2. upload to ftp server
	// InputStream <------> byte[] simple and See API

	public boolean uploadFile(String fileFullPath, String newName) throws IOException {
		boolean flag = false;
		InputStream iStream = null;
		try {
			iStream = new FileInputStream(fileFullPath);
			String encodingName = new String(newName.getBytes("UTF-8"), "ISO-8859-1");
			flag = super.getObject().storeFile(encodingName, iStream);

		} finally {
			if (iStream != null) {
				iStream.close();
			}
		}
		return flag;
	}

	public boolean uploadFile(String fileName) throws IOException {
		return uploadFile(fileName, fileName);
	}

	public boolean uploadFile(InputStream iStream, String newName) throws IOException {
		boolean flag = false;
		try {
			// can execute [OutputStream storeFileStream(String remote)]
			// Above method return's value is the local file stream.
			flag = super.getObject().storeFile(newName, iStream);
		} catch (IOException e) {
			flag = false;
			return flag;
		} finally {
			if (iStream != null) {
				iStream.close();
			}
		}
		return flag;
	}

	/***
	 * @上传文件夹
	 * @param localDirectory
	 *            当地文件夹
	 * @param remoteDirectoryPath
	 *            Ftp 服务器路径 以目录"/"结束
	 * @throws IOException
	 */
	public boolean uploadDirectory(String localDirectory, String remoteDirectoryPath) throws IOException {
		File src = new File(localDirectory);
		// remoteDirectoryPath = remoteDirectoryPath + src.getName() + "/";
		super.getObject().makeDirectory(remoteDirectoryPath);
		super.getObject().changeWorkingDirectory(remoteDirectoryPath);

		// ftpClient.listDirectories();
		File[] allFile = src.listFiles();
		for (int currentFile = 0; currentFile < allFile.length; currentFile++) {
			if (!allFile[currentFile].isDirectory()) {
				String srcFilePath = allFile[currentFile].getPath().toString();
				log.debug("ApacheFtpUtil.upload file: " + srcFilePath);
				uploadFile(srcFilePath, allFile[currentFile].getName());
			}
		}
		for (int currentFile = 0; currentFile < allFile.length; currentFile++) {
			if (allFile[currentFile].isDirectory()) {
				// 递归
				String subRemoteDirPath = remoteDirectoryPath + "/" + allFile[currentFile].getName();
				uploadDirectory(allFile[currentFile].getPath().toString(), subRemoteDirPath);
			}
		}
		return true;
	}

	// #3. Down load

	public boolean download(String remoteFileName, String localFileName) throws IOException {
		boolean flag = false;
		File outfile = new File(localFileName);
		OutputStream oStream = null;
		try {
			oStream = new FileOutputStream(outfile);
			flag = super.getObject().retrieveFile(remoteFileName, oStream);
		} catch (IOException e) {
			flag = false;
			return flag;
		} finally {
			if (oStream != null) {
				oStream.close();
			}
		}
		return flag;
	}

	public InputStream downFile(String sourceFileName) throws IOException {
		return super.getObject().retrieveFileStream(sourceFileName);
	}

	// #4. FtpClient info
	public int getReplyCode() {
		return super.getObject().getReplyCode();
	}

	public String[] getReplyStringArray() {
		return super.getObject().getReplyStrings();
	}

	public String getReplyStrings() {
		StringBuffer buffer = new StringBuffer();
		String[] array = super.getObject().getReplyStrings();
		for (String str : array) {
			buffer.append(str).append("\n");
		}
		return buffer.toString();
	}

	public String getReplyString() {
		return super.getObject().getReplyString();
	}
	
}
