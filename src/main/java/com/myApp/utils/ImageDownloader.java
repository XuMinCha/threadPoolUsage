/**
 * 
 */
package com.myApp.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;


/**
 * @author Administrator
 *
 */
public class ImageDownloader {
	private static final String USER_AGENT = "Mozilla/5.0 Firefox/26.0";

	private static Logger logger = LoggerFactory.getLogger(ImageDownloader.class);

	private static final int TIMEOUT_SECONDS = 120;

	private static final int POOL_SIZE = 120;

	private CloseableHttpClient httpclient;

	public void initApacheHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		// Create global request configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(TIMEOUT_SECONDS * 1000)
				.setConnectTimeout(TIMEOUT_SECONDS * 1000).build();

		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				return true;
			}
		}).build();

		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
		// Create an HttpClient with the given custom dependencies and
		// configuration.
		httpclient = HttpClients.custom().setUserAgent(USER_AGENT).setMaxConnTotal(POOL_SIZE)
				.setMaxConnPerRoute(POOL_SIZE).setDefaultRequestConfig(defaultRequestConfig).setSSLSocketFactory(sslsf)
				.build();
	}

	public void destroyApacheHttpClient() {
		try {
			httpclient.close();
		} catch (IOException e) {
			logger.error("httpclient close fail", e);
		}
	}

	public boolean fetchContent(String imageUrl, String _saveDir, String _fileName)
			throws ClientProtocolException, IOException {
		/*File localDir = new File(_saveDir);
		if(!localDir.exists()){
			localDir.mkdirs();
		}*/
		
		HttpGet httpget = new HttpGet(imageUrl);
		// httpget.setHeader("Referer", "http://www.google.com");

		System.out.println("executing request " + httpget.getURI());
		CloseableHttpResponse response = httpclient.execute(httpget);

		try {
			HttpEntity entity = response.getEntity();

			if (response.getStatusLine().getStatusCode() >= 400) {
				throw new IOException("Got bad response, error code = " + response.getStatusLine().getStatusCode()
						+ " imageUrl: " + imageUrl);
			}
			if (entity != null) {
				String fileName = StringUtils.isEmpty(_fileName) ? StringUtils.getFilename(imageUrl) : _fileName;
				try (InputStream input = entity.getContent()) {
					try (OutputStream output = new FileOutputStream(new File(_saveDir + "/" + fileName))) {
						IOUtils.copy(input, output);
						output.flush();
						return true;
					}
				}
			}
		} finally {
			response.close();
		}
		return false;
    }

    public boolean fetchContent(InputStream input, String saveDir, String fileName) throws FileNotFoundException, IOException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(new File(saveDir + "/" + fileName));
            IOUtils.copy(input, output);
            output.flush();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.error("从S3下载文件失败：" + fileName + "未提前创建");
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("从S3下载文件失败", e);
            throw e;
        } finally {
            output.close();
            input.close();
        }
    }
	// public static void main(String[] args) {
	// String fileName=StringUtils.getFilename("http://abc.com/ab.jpg");
	// System.out.println(fileName);
	// }
}
