/**
 * 
 */
package com.myApp.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
public class S3FileService {

	private Log log = LogFactory.getLog(S3FileService.class);

	//private MinioClient minioClient;

	private String bucketName;

	public String getUrlOfBucket(String _bucketName, String fullPath) throws Exception {
		String url = ""; //此处从s3获得影像url
		//String url = minioClient.presignedGetObject(_bucketName, fullPath); // 最多7天有效时长
		log.info("------获取到文件的url------:" + fullPath + ":" + url);
		return url;
	}

  
}
