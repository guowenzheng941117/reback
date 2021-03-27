package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
public class Controller {
	@RequestMapping("/uploadFile")
	public void uploadFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		System.out.println("------------------上传文件------------------");
		System.out.println("---------------" + request.getParameter("ip") + "----------");
		System.out.println("-----------------------------------------");

		MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
		List<MultipartFile> files = multipartHttpServletRequest.getFiles("file");

		String suffix = request.getParameter("suffix");
		String preffix = request.getParameter("preffix");
		String uploadPath = "E:/allfiles/" + request.getParameter("ip") + "/" + request.getParameter("dirpath");

		File folder = new File(uploadPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		// 文件上传组件
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setHeaderEncoding("UTF-8");

		for (MultipartFile fileItem : files) {
			// 处理保存文件名称 这里是多余的 可把下面的fileName直接替换为preffix
			String fileName = fileItem.getName();
			fileName = preffix;

			// 判断文件是否存在
			String filepath = uploadPath + "/" + fileName + request.getParameter("time") + "." + suffix;
			File file = new File(filepath);
			if (file.exists()) {
				System.out.println("文件已经存在");
				continue;
			}

			// io 输入流读文件
			InputStream is = fileItem.getInputStream();
			// 利用输出流写入对应的文件夹
			FileOutputStream fout = new FileOutputStream(new File(filepath));
			// 使用流返回把返回的数据写入
			PrintWriter outputStream = response.getWriter();

			// 写入数据
			byte[] buffer = new byte[1024];
			int len = 0;
			while ((len = (is.read(buffer))) > -1) {
				fout.write(buffer, 0, len);
			}

			fout.close();
			is.close();

			System.out.println("path:" + uploadPath + "/" + fileName + suffix);
			// 这里就是返回前端的字符串 记得设置字符编码 防止乱码
			outputStream.write("路径:" + uploadPath + "/" + fileName + suffix);
			outputStream.flush();
			outputStream.close();
		}

	}

	@RequestMapping("/uploadIndex")
	public String uploadIndex() {

		return "hello world";
	}

	@RequestMapping("/hasFile")
	public boolean hasFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		System.out.println("----------------判断是否有文件----------------");
		System.out.println("---------------" + request.getParameter("ip") + "----------");
		System.out.println("-----------------------------------------");

		String suffix = request.getParameter("suffix");
		String preffix = request.getParameter("preffix");
		String uploadPath = "E:/allfiles/" + request.getParameter("ip") + "/" + request.getParameter("dirpath");

		// 判断文件是否存在
		String filepath = uploadPath + "/" + preffix + request.getParameter("time") + "." + suffix;
		System.out.println(filepath);
		File file = new File(filepath);
		if (file.exists()) {
			System.out.println("文件存在");
			return true;
		}
		System.out.println("文件不存在");
		return false;
	}

	@RequestMapping("/updateExe")
	public ResponseEntity<InputStreamResource> updateExe() throws FileNotFoundException {
		File file = new File("D:\\allfiles\\up.exe");
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Content-Disposition", "attachment; filename=" + System.currentTimeMillis() + ".xls");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		headers.add("Last-Modified", new Date().toString());
		headers.add("version", "1");

		return ResponseEntity.ok().headers(headers).contentLength(file.length())
				.contentType(MediaType.parseMediaType("application/octet-stream")).body(new InputStreamResource(new FileInputStream(file)));
	}
}
