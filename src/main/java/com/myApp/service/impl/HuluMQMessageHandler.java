/**
 *
 */
package com.myApp.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myApp.ftp.FTPClientFactory;
import com.myApp.ftp.FTPClientPool;
import com.myApp.ftp.FTPPooledObject;
import com.myApp.ftp.NewFTPConfig;
import com.myApp.model.AppAttachmentGroup;
import com.myApp.model.ApplyOrder;
import com.myApp.model.Attachment;
import com.myApp.service.MQMessageHandler;
import com.myApp.utils.DateUtils;
import com.myApp.utils.EasyScanService;
import com.myApp.utils.EasyScanServicePortType;
import com.myApp.utils.ImageDownloader;
import com.myApp.utils.ThreadPool;

/**
 * @author xuemc
 * 此类采用ThreadPoolExecutor和ForkJoinPool嵌套使用，结合业务场景提供一种新思路，提高并行处理效率
 */
@Service
public class HuluMQMessageHandler implements MQMessageHandler,ApplicationContextAware,InitializingBean,DisposableBean {

    private Logger log = LoggerFactory.getLogger(getClass());

    private HuluMQMessageHandler proxy;
    
    @Autowired
    NewFTPConfig newFtpConfig;
    
    @Autowired
    private S3FileService s3;

    private static final String S3_KYP_BUCKET_NAME = "kuayipan";
    
    @Value("${imagesystem.ImageSystemUserID}")
    private String ImageSystemUserID;
    @Value("${imagesystem.ImageSystemPassword}")
    private String ImageSystemPassword;

    @Value("${newImageSystem.ImageSystem}")
    private String newImageSystemUrl;
    
    private Lock lock = new ReentrantLock();
    
    //上传影像专用线程池
    private ExecutorService imageExecutor;
    
    //影像件上传分治任务线程池
    private ForkJoinPool pool;
    
    //FTPClient对象池
    private FTPClientPool ftpClientPool;
    
    //从s3下载影像
    private ImageDownloader imgDownloader;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    	this.proxy = applicationContext.getBean(HuluMQMessageHandler.class);
	}
    
    @Override
    public void handleMessage(JSONObject receivedJson) {
    	ThreadPool.getInstance().execute(() -> 
    	     processNewOrder(receivedJson, applyOrder -> {
            	 int retry = 0;
	             try {
	            	 log.info("开始上传影像平台,id:{},附件大类数量:{}",applyOrder.getSerialNo(),applyOrder.getAppAttachmentGroupList().size());
	                 transImageToPlatform(applyOrder.getSerialNo(), applyOrder.getAppAttachmentGroupList());
	             } catch (Exception e) {
	                 log.error("处理影像附件时异常:" + e.getMessage(), e);
	                 //如果文件没找到，不必重试
	                 if (e instanceof FileNotFoundException) {
	                	 throw new RuntimeException(e);
	                 }
	                 //否则重试3次
	                 retryTransImageToPlatform(applyOrder.getSerialNo(),++retry,3,applyOrder.getAppAttachmentGroupList());
	             }
	             // 上传影像平台成功后，再入库
	             try {
	            	 log.info("入库的申请单:" + JSONObject.toJSONString(applyOrder));
					 //ahRepository.saveApplyHb(applyHb);
				 } catch (Exception e) {
					 log.error("申请信息入库异常:"+e.getMessage(), e);
					 throw new RuntimeException(e);
				 }
	             log.info("id:{}上传影像平台和入库成功",applyOrder.getSerialNo());
	             return "success";
			 })
        );

    }

    /**
     * 上传影像平台失败重试
     * @param orderId    申请单id
     * @param retry   	   第retry次重试
     * @param limit   	   最大重试次数
     * @param imageList  要上传的影像列表
     */
    private void retryTransImageToPlatform(String orderId,int retry,int limit,List<AppAttachmentGroup> imageList) {
    	if(retry <= limit){
    		try {
    			Thread.sleep(retry * 600000); //第一次10分钟后重试,第二次20分钟后重试,第三次30分钟后重试...
    			log.info("上传影像平台重试第{}次,信审id:{}",retry,orderId);
    			transImageToPlatform(orderId, imageList);
    		} catch (Exception e) {
    			log.error("重试第"+retry+"次上传影像附件异常:"+e.getMessage(), e);
    			if (retry == limit) {
    				throw new RuntimeException(e);
    			}
    			retryTransImageToPlatform(orderId,++retry,limit,imageList);
    		}
    	}
    }
             
    /**
     *  处理新订单
     */
    public boolean processNewOrder(JSONObject receivedJson, Function<ApplyOrder,String> callback){
    	String applyId = null;
        try{
            
            JSONObject applyLoan = receivedJson.getJSONObject("applyLoan");
            applyId = applyLoan.getString("applyId");
            
            // 2 进件前风控监测;

            // 3、生成正式申请编号
			/*
			 * String orderId = generateOrderId(receivedJson);
			 * log.info("进件->applyId:{} 对应生成的信审id:{}",applyId,orderId);
			 */
            
            // 4 组装申请单信息
            ApplyOrder ah = assembleApplyHb(receivedJson);

            callback.apply(ah);
            return true;
        }
       
        catch(Exception ex){
        	log.error("进件->processHuluNewOrder异常:",ex);
        	throw ex;
        }
    }
    
    private ApplyOrder assembleApplyHb(JSONObject receivedJson) {
    	ApplyOrder ah = new ApplyOrder();
    	 // 5.1、客户信息
        ah.setSerialNo("B8989");

        // 5.11、影像资料信息
        ah.setAppAttachmentGroupList(new ArrayList<>());
        
        return ah;
    }
    
    /**
     *  分治任务包括从s3服务器下载影像和上传到ftp服务器
     */
    private class FetchAttachmentTask extends RecursiveTask<List<String>> {
    	
		private static final long serialVersionUID = 1L;

		//单个线程一次性处理的影像阈值
		private int THRESHOLD = 3;
    	
    	private List<Attachment> attachments;
    	
    	private ImageDownloader imgDownloader;
    	
    	private FTPPooledObject ftpPooledObject;
    	
    	private String tempDirName;
    	
    	private String orderId;
    	
    	public FetchAttachmentTask(List<Attachment> attachments,ImageDownloader imgDownloader,FTPPooledObject ftpPooledObject,String tempDirName,String orderId){
    		this.attachments = attachments;
    		this.imgDownloader = imgDownloader;
    		this.ftpPooledObject = ftpPooledObject;
    		this.tempDirName = tempDirName;
    		this.orderId = orderId;
    	}
    	
		@Override
		protected List<String> compute() {
			List<String> result = new ArrayList<String>();
			if (this.attachments.size() <= this.THRESHOLD) {
				 for (Attachment attach : attachments) {
					 try {
						 String url = s3.getUrlOfBucket(S3_KYP_BUCKET_NAME, attach.getFileLocation());
						 if (StringUtils.isEmpty(attach.getFileName())) {// 生成随机的文件名
		                     attach.setFileName(RandomStringUtils.randomAlphanumeric(6) + ".jpg");
		                 }
		                 boolean succ = imgDownloader.fetchContent(url, tempDirName, attach.getFileName());
		                 if (!succ) {
		                     throw new RuntimeException("下载附件失败。【申请编号:" + orderId + ",type:" + attach.getCheckDataType()
		                             + ",location:" + attach.getFileLocation() + ",fileName:" + attach.getFileName() + ";url:"
		                             + url + "】");
		                 }
		                 
		                 synchronized(ftpPooledObject){
		                	 boolean ftpSuc = ftpPooledObject.uploadFile(tempDirName + "/" + attach.getFileName(), attach.getFileName());
			                 if (!ftpSuc) {
			                     throw new RuntimeException("上传附件至FTP失败。【申请编号:" + orderId + ",type:" + attach.getCheckDataType()
			                             + ",location:" + attach.getFileLocation() + ",fileName:" + attach.getFileName() + ";url:"
			                             + url + "】");
			                 }
			                 log.info("影像{}上传ftp服务器成功.",attach.getFileName());
			                 result.add(attach.getFileName());
		                 }
					 } catch (Exception e) {
						 log.error(e.getMessage(),e);
						 if(e instanceof RuntimeException){
							 throw (RuntimeException)e;
						 }
						 throw new RuntimeException(e);
					 }
	             }
			}
			else{
				//单个大类下附件数量超过THRESHOLD则进行拆分
				int total = this.attachments.size();
				int num = total/THRESHOLD;
				List<FetchAttachmentTask> subTasks = new ArrayList<FetchAttachmentTask>();
				for(int i=0;i<=num;i++){
					List<Attachment> list = new ArrayList<Attachment>();
					for(int j = i*THRESHOLD;j < (i+1)*THRESHOLD;j++){
						if (j >= (total)) {
							break;
						}
						Attachment ele = attachments.get(j);
						if (ele != null) {
							list.add(ele);
						}
					}
					FetchAttachmentTask subTask = new FetchAttachmentTask(list,imgDownloader,ftpPooledObject,tempDirName,orderId);
					subTasks.add(subTask);
				}
				
				invokeAll(subTasks);
				
				subTasks.stream().forEach(task -> {
					try {
						result.addAll(task.get());
					}
					catch (Exception e) {
						log.error("subTasks:"+e.getMessage(),e);
						if (e instanceof RuntimeException) {
							 throw (RuntimeException)e;
						}
						throw new RuntimeException(e);
					}
				});
			}
			return result;
		}
    }

    
    /**
     * 上传新影像平台
     *
     * @return
     * @throws Exception
     */
    public boolean transImageToPlatform(String orderId, Collection<AppAttachmentGroup> attachments) throws Exception {
    	FTPPooledObject ftpPooledObject = (FTPPooledObject)ftpClientPool.borrowObject();
        try {
        	//生成本地临时路径
            String localTempDirName = "./upload/" + orderId;
            File tempDir = new File(localTempDirName);
            if(!tempDir.exists()){
            	tempDir.mkdirs();
            }
            
            //切换远程路径
            String remoteDirName = "/upload/" + orderId;
            if(!ftpPooledObject.changeDirectory(remoteDirName)){
	       		if(!ftpPooledObject.createDirectory(remoteDirName)){
	       			throw new RuntimeException("创建远程文件目录"+remoteDirName+"失败!");
	       		}
	       		ftpPooledObject.changeDirectory(remoteDirName);
       	    }
            long start = System.currentTimeMillis();
            try {
                CompletionService<List<JSONObject>> completionService = new ExecutorCompletionService<List<JSONObject>>(imageExecutor);
                AtomicInteger imageNum = new AtomicInteger(0);
                
                attachments.stream().forEach(group -> {
                	imageNum.accumulateAndGet(group.getAttaches().size(), (count,increment) -> count+=increment);
                	//将单个大类上传影像平台任务交由ThreadPoolExecutor线程池执行
                	completionService.submit(() -> {
                  		String checkDataType = group.getCheckDataType();
                  		
                        //将下载影像和上传ftp服务器任务交由ForkJoinPool线程池处理
                        ForkJoinTask<List<String>> resultFuture = pool.submit(new FetchAttachmentTask(group.getAttaches(),imgDownloader,ftpPooledObject,localTempDirName,orderId));
                        try{
                        	List<String> fileNames = (List<String>)resultFuture.get();
                         	log.info(Thread.currentThread().getName()+"上传ftp文件个数:"+fileNames.size());
                         	
                         	return fileNames.stream().map(fileName -> {
							                         	    JSONObject file = new JSONObject();
							                                file.put("dirCode", checkDataType);
							                                file.put("name", fileName);
							                                return file;
                                                         }).collect(Collectors.toList());
                        }
                        catch(Exception e){
                        	log.error(e.getMessage(),e);
                         	throw e;
                        }
                    });
                });
                
                JSONArray fileArray = new JSONArray();
                for (int i=0;i<attachments.size();i++) {
                  	try{
              			List<JSONObject> result = completionService.take().get();
              			fileArray.addAll(result);
                  	}
                  	catch (Exception e) {
                  		log.error(e.getMessage(),e);
                  		//找出最开始异常
                  		Throwable tmp,t = null;
                  		for(tmp = e.getCause();tmp != null;){
                  			t = tmp;
                  			tmp = tmp.getCause();
                  		}
                  		throw (Exception)t;
                  	}
                }
                
                JSONObject params = new JSONObject();
                params.put("applyNo",orderId);
                params.put("channel", "FK001"); //固定自有平台
                params.put("createUser","HULU");
                params.put("path", remoteDirName);
                params.put("fileList", fileArray);
                
                long s = System.currentTimeMillis();
                String uploadUrl = newImageSystemUrl+"/upload";
                log.info("调新影像平台{},请求参数:{}",uploadUrl,params.toJSONString());
                String restResponse = restTemplate.postForObject(uploadUrl, params.toJSONString(), String.class);
                log.info("调新影像平台{},返回结果:{}",uploadUrl,restResponse);
                long e = System.currentTimeMillis();
                log.debug("调"+newImageSystemUrl+"耗时:"+(e-s)+"毫秒");
                
                JSONObject response = JSONObject.parseObject(restResponse);
                if (0 != response.getInteger("code")) {
                    throw new RuntimeException(
                             "上传新影像平台失败。【申请编号:" + orderId + ",RESP:" + response.getString("message") + "】");
                }

                long end = System.currentTimeMillis();
                log.info("新影像平台--ThreadPoolExecutor+ForkJoinPool共上传影像:{}张,耗时:{}毫秒",imageNum,(end-start));
            } finally {
                FileUtils.deleteDirectory(tempDir);
                //imgDownloader.destroyApacheHttpClient();
            }
            return true;
        } finally {
        	ftpClientPool.returnObject(ftpPooledObject);
        }
    }
    
    private class FetchAttachmentTaskOld extends RecursiveTask<String>{
    	
		private static final long serialVersionUID = 1L;

		//单个线程一次性处理的影像阈值
		private int THRESHOLD = 3;
    	
    	private List<Attachment> attachments;
    	
    	private ImageDownloader imgDownloader;
    	
    	private FTPPooledObject ftpPooledObject;
    	
    	private String tempDirName;
    	
    	private String orderId;
    	
    	private AtomicInteger itemNo;
    	
    	public FetchAttachmentTaskOld(List<Attachment> attachments,ImageDownloader imgDownloader,FTPPooledObject ftpPooledObject,String tempDirName,String orderId,AtomicInteger itemNo){
    		this.attachments = attachments;
    		this.imgDownloader = imgDownloader;
    		this.ftpPooledObject = ftpPooledObject;
    		this.tempDirName = tempDirName;
    		this.orderId = orderId;
    		this.itemNo = itemNo;
    	}
    	
		@Override
		protected String compute(){
			StringBuffer pageSb = new StringBuffer();
			if(this.attachments.size() <= this.THRESHOLD){
				 for (Attachment attach : attachments) {
					 try {
						 String url = s3.getUrlOfBucket(S3_KYP_BUCKET_NAME, attach.getFileLocation());
						 if (StringUtils.isEmpty(attach.getFileName())) {// 生成随机的文件名
		                     attach.setFileName(RandomStringUtils.randomAlphanumeric(6) + ".jpg");
		                 }
		                 boolean succ = imgDownloader.fetchContent(url, tempDirName, attach.getFileName());
		                 if (!succ) {
		                     throw new RuntimeException("下载附件失败。【申请编号:" + orderId + ",type:" + attach.getCheckDataType()
		                             + ",location:" + attach.getFileLocation() + ",fileName:" + attach.getFileName() + ";url:"
		                             + url + "】");
		                 }
		                 
		                 synchronized(ftpPooledObject){
		                	 boolean ftpSuc = ftpPooledObject.uploadFile(tempDirName + "/" + attach.getFileName(), attach.getFileName());
			                 if (!ftpSuc) {
			                     throw new RuntimeException("上传附件至FTP失败。【申请编号:" + orderId + ",type:" + attach.getCheckDataType()
			                             + ",location:" + attach.getFileLocation() + ",fileName:" + attach.getFileName() + ";url:"
			                             + url + "】");
			                 }
			                 pageSb.append("<PAGE>");
			                 pageSb.append("<PAGENO>").append(itemNo.getAndIncrement()).append("</PAGENO>");
			                 pageSb.append("<PAGENAME>").append(attach.getFileName()).append("</PAGENAME>");
			                 pageSb.append("<PAGEPATH>/</PAGEPATH>");
			                 pageSb.append("</PAGE>");
		                 }
					 } catch (Exception e) {
						 log.error(e.getMessage(),e);
						 if(e instanceof RuntimeException){
							 throw (RuntimeException)e;
						 }
						 throw new RuntimeException(e);
					 }
	             }
			}
			else{
				//单个大类下附件数量超过THRESHOLD则进行拆分
				int total = this.attachments.size();
				int num = total/THRESHOLD;
				List<FetchAttachmentTaskOld> subTasks = new ArrayList<FetchAttachmentTaskOld>();
				for(int i=0;i<=num;i++){
					List<Attachment> list = new ArrayList<Attachment>();
					for(int j=i*THRESHOLD;j<(i+1)*THRESHOLD;j++){
						if(j >= (total)){
							break;
						}
						Attachment ele = attachments.get(j);
						if(ele != null){
							list.add(ele);
						}
					}
					FetchAttachmentTaskOld subTask = new FetchAttachmentTaskOld(list,imgDownloader,ftpPooledObject,tempDirName,orderId,itemNo);
					subTasks.add(subTask);
				}
				
				invokeAll(subTasks);
				
				for(FetchAttachmentTaskOld task : subTasks){
					try {
						String pageTag = task.get();
						pageSb.append(pageTag);
					} 
					catch (Exception e) {
						log.error(Thread.currentThread().getName()+"异常:"+e.getMessage(),e);
						throw new RuntimeException(e);
					}
				}
			}
			log.info(Thread.currentThread().getName()+" 收集的PageTag:"+pageSb);
			return pageSb.toString();
		}
    }

    /**
     * 上传旧影像平台(26张图片上传测试5次最快纪录2743ms,2772ms,2305ms,4111ms,3750ms
     * 			 40张图片上传测试5次最快纪录5386ms,4717ms,4413ms,4067ms,4007ms)
     *
     * @return
     * @throws Exception
     */
    public boolean transImageToPlatformOld(String orderId, Collection<AppAttachmentGroup> attachments) throws Exception {
    	FTPPooledObject ftpPooledObject = (FTPPooledObject)ftpClientPool.borrowObject();
        try {
        	EasyScanService service = new EasyScanService();
        	EasyScanServicePortType ept = service.getEasyScanServiceHttpSoap11Endpoint();
        	
            String tempDirName = "./" + RandomStringUtils.randomAlphanumeric(8);
            File tempDir = new File(tempDirName);
            tempDir.mkdir();
            long start = System.currentTimeMillis();
            try {
                String bussType = orderId.startsWith("B") ? "002" : "001";
                String date = DateUtils.getDateTime(new Date(), DateUtils.DEFUALT_LONG_TIME_FORMAT);
                CompletionService<String> completionService = new ExecutorCompletionService<String>(imageExecutor);
                int imageNum = 0;
                for (AppAttachmentGroup group : attachments) {
                	imageNum += group.getAttaches().size();
                	//将单个大类上传影像平台任务交由ThreadPoolExecutor线程池执行
                	completionService.submit(() -> {
                      		String checkDataType = group.getCheckDataType();
                            int imgNum = group.getAttaches().size();

                            StringBuffer xml = new StringBuffer("<TRANSDATA>");
                            xml.append("<TRANSHEAD>");
                            xml.append("<VERSION>1.0</VERSION>");
                            xml.append("<SYSCODE>001</SYSCODE>");
                            xml.append("<TRANUSER>").append(ImageSystemUserID).append("</TRANUSER>");
                            xml.append("<TRANPASSWD>").append(ImageSystemPassword).append("</TRANPASSWD>");
                            xml.append("<TRANSCODE>80012</TRANSCODE>");
                            xml.append("</TRANSHEAD>");

                            xml.append("<TRANSBODY>");
                            xml.append("<DOC>");

                            xml.append("<DOCCODE>").append(orderId).append("</DOCCODE>");
                            xml.append("<GROUPNO>").append(orderId).append("</GROUPNO>");
                            xml.append("<CHANNEL>1</CHANNEL>");
                            xml.append("<BUSSTYPE>").append(bussType).append("</BUSSTYPE>");
                            xml.append("<SUBTYPE>").append(checkDataType).append("</SUBTYPE>");
                            xml.append("<NUMPAGES>").append(imgNum).append("</NUMPAGES>");
                            xml.append("<MANAGECOM>").append("2015020800000004").append("</MANAGECOM>");// 录入机构
                            xml.append("<SCANNO>0</SCANNO>");
                            xml.append("<SCANOPERATOR>").append("APP").append("</SCANOPERATOR>");// 录入人员
                            xml.append("<SCANDATE>").append(date).append("</SCANDATE>");
                            xml.append("<DOCID></DOCID>");
                             
                            //将下载影像和上传ftp服务器任务交由ForkJoinPool线程池处理
                            ForkJoinTask<?> resultFuture = pool.submit(new FetchAttachmentTaskOld(group.getAttaches(),imgDownloader,ftpPooledObject,tempDirName,orderId,new AtomicInteger(1)));
                            try{
                             	String pages = (String)resultFuture.get();
                             	log.info(Thread.currentThread().getName()+"一次性上传的page tags:"+pages);
                             	xml.append(pages);
                                xml.append("</DOC>");
                                xml.append("</TRANSBODY>");
                                xml.append("</TRANSDATA>");
                            }
                            catch(Exception e){
                            	log.error(e.getMessage(),e);
                             	throw new RuntimeException(e);
                            }
                             
                            long s = System.currentTimeMillis();
                            String wsResponse = ept.easyScanInterface(xml.toString());
                             
                            long e = System.currentTimeMillis();
                            log.debug(Thread.currentThread().getName()+"上传影像耗时:"+(e-s)+"毫秒");
                            boolean success = getUploadResposeStr(wsResponse);
                            //log.info("  上传影像平台，类型：" + checkDataType + "，材料数量：" + group.getAttaches().size());
                            if (!success) {
                                throw new RuntimeException(
                                         "上传影像平台失败。【申请编号:" + orderId + ",type:" + checkDataType + ";RESP:" + wsResponse + "】");
                            }
                            return Thread.currentThread().getName()+"上传影像平台成功，类型：" + checkDataType + "，材料数量：" + group.getAttaches().size();
                     });
                }
                for(int i=0;i<attachments.size();i++){
                  	try{
              			String result = completionService.take().get();
                      	log.info(result.toString());
                  	}
                  	catch(Exception e){
                  		log.error(e.getMessage(),e);
                  		//找出最开始异常
                  		Throwable tmp,t = null;
                  		for(tmp = e.getCause();tmp != null;){
                  			t = tmp;
                  			tmp = tmp.getCause();
                  		}
                  		throw (Exception)t;
                  	}
                }
                long end = System.currentTimeMillis();
                log.info("ThreadPoolExecutor+ForkJoinPool共上传影像:{}张,耗时:{}毫秒",imageNum,(end-start));
            } finally {
                FileUtils.deleteDirectory(tempDir);
                //imgDownloader.destroyApacheHttpClient();
            }
            return true;
        } finally {
        	ftpClientPool.returnObject(ftpPooledObject);
        }
    }
  
    private boolean getUploadResposeStr(String sReturn) throws JDOMException, IOException {
        boolean vReturn = false;

        StringReader xmlReader = new StringReader(sReturn);
        InputSource xmlSource = new InputSource(xmlReader);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlSource);
        Element element = doc.getRootElement();
        Element TRANSBODY = element.getChild("TRANSBODY");
        Element TRANSRESULT = TRANSBODY.getChild("TRANSRESULT");
        Element RETURNCODE = TRANSRESULT.getChild("RETURNCODE");
        String sRETURNCODE = RETURNCODE.getText();
        if (sRETURNCODE.equals("000000")) {
            vReturn = true;
        }
        return vReturn;
    }
    
	@Override
	public void afterPropertiesSet() throws Exception {
        ThreadPool.HBThreadFactory threadFactory = new ThreadPool.HBThreadFactory(false);
        threadFactory.setNamePrefix("imageReactorThread-");
        int nThread = Runtime.getRuntime().availableProcessors() << 1;
        imageExecutor = new ThreadPoolExecutor(nThread,nThread,0L,TimeUnit.MILLISECONDS,
        								       new LinkedBlockingQueue<Runnable>(),threadFactory);
        
        pool = new ForkJoinPool(nThread);
        
        Assert.notNull(newFtpConfig, "newFtpConfig不能为null!");
        //初始化FTPClient对象池
        FTPClientFactory ftpClientFactory = new FTPClientFactory(newFtpConfig);
        ftpClientPool = new FTPClientPool(3,ftpClientFactory);
        
        imgDownloader = new ImageDownloader();
        imgDownloader.initApacheHttpClient();
	}

	@Override
	public void destroy() throws Exception {
		if(imageExecutor != null){
		    imageExecutor.shutdown();
		}
		if(pool != null){
		    pool.shutdown();
		}
		if(ftpClientPool != null){
			ftpClientPool.close();
		}
		if(imgDownloader != null){
			imgDownloader.destroyApacheHttpClient();
		}
	}

	@Override
	public String getMessageSource() {
		return "hl";
	}

	@Override
	public String isPotentialCustomer() {
		return "NOT";
	}

}
