package com.hsjry.mavenplugin;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.hsjry.mavenplugin.linux.ssh.SSHUtils;
import com.jcraft.jsch.HostKey;

@Mojo( name = "loanDeploy")
public class LoanDeployMojo extends AbstractMojo {
	
	@Parameter(property="h")
	private String host;
	@Parameter(property="port",defaultValue="22")
	private String port;
	@Parameter(property="u")
	private String user;
	@Parameter(property="p")
	private String pwd;
	@Parameter(property="restart")
	private boolean restart;
	/**
	 * �ϴ�ָ����war�����ƣ�ϵͳ�ὫwarName��sftpFileProperties����war�����ƽ���ƥ�䣩.�������Ϊ�գ���Ĭ���ϴ�sftpFileProperties�������õ����а�
	 */
	@Parameter(property="m")
	private String warName;
	
	/**
	 * ������Ŀ¼�µ��ĵ��ϴ���linux��Ŀ¼ key�Ǳ���Ŀ¼��value��linux�ϵ�Ŀ¼
	 */
	@Parameter
	private Properties rsyncDirectoryProperties;
	/**
	 * �������ļ��ϴ���linux��,key�Ǳ����ļ���value��linux�ϵ��ļ�
	 */
	@Parameter
	private Properties warFileProperties;
	
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = super.getLog();
//		Map pluginContext = super.getPluginContext(); 
//		Set<Map.Entry<Object,Object>> entrySet = pluginContext.entrySet();
//		for(Map.Entry<Object,Object> entry:entrySet){
//			log.info("hsjry maven plugin:"+entry.getKey()+" :::: "+entry.getValue());
//		}
//		log.info("==================");
//		Properties properties = System.getProperties();
//		Set<Entry<Object, Object>> proEntry = properties.entrySet();
//		for(Map.Entry<Object, Object> entry:proEntry){
//			log.info("hsjry maven plugin:"+entry.getKey()+" :::: "+entry.getValue());
//		}
//		log.info("==================");
//		URL resource = this.getClass().getClassLoader().getResource("./");
//		log.info("My plugin 111:"+resource); 
//		log.info("==================");
//		log.info(this.toString());
		SSHUtils.connectSSH(host, new Integer(port), user, pwd); 
		putAndRestart();
		SSHUtils.disconnect();
	}

	/**
	 * ��war���ϴ���Linux,��������ط��� 
	 * @param host
	 */
	private void putAndRestart(){
		if(warName==null||warName.trim().equals("")){//�ϴ����õ����а�
			Set<Entry<Object, Object>> entrySet = warFileProperties.entrySet();
			for(Entry<Object, Object> entry:entrySet){
				String sourceFileStr = entry.getKey()+"";
				String targetFile = entry.getValue()+"";
				this.put(sourceFileStr, targetFile);
			}
		}else{
			Set<Entry<Object, Object>> entrySet = warFileProperties.entrySet();
			for(Entry<Object, Object> entry:entrySet){
				String sourceFileStr = entry.getKey()+"";
				String targetFile = entry.getValue()+"";
				File sourceFile = new File(sourceFileStr);
				if(sourceFile.exists()&&sourceFile.isDirectory()){
					File warFile = new File(sourceFile,warName);
					if(!warFile.exists()){
						continue;
					}
				}else{
					String name = sourceFile.getName();
					if(!warName.equals(name)){
						continue;
					}
				}
				this.put(sourceFileStr, targetFile);
			}
		}
	}
	 
	private void put(String sourceFileStr,String  targetFile){
		File sourceFile = new File(sourceFileStr);
		if(!sourceFile.exists()){
			throw new RuntimeException(sourceFileStr+"Ŀ¼������");
		}
		if(sourceFile.isDirectory()){
			File[] listFiles = this.listWar(sourceFile);
			if(listFiles==null||listFiles.length!=1){
				throw new RuntimeException(sourceFile+"�ҵ�"+listFiles.length+"��war��������ֵ��1��");
			}
			sourceFile = listFiles[0];
		}
		boolean linuxDirectIsExists = SSHUtils.linuxDirectIsExists(targetFile);
		if(!linuxDirectIsExists){
			SSHUtils.mkdirs(targetFile);
		}
		super.getLog().info("put file "+sourceFileStr+"-->" +targetFile+" begin...");
		SSHUtils.sftpPut(sourceFileStr, targetFile);
		super.getLog().info("put file "+sourceFileStr+"-->" +targetFile+" finished");
		if(this.restart){
			String shutdownSHDirectory = targetFile+"/../bin/";
			String shutdownSHFileName = "shutdown.sh";
			boolean linuxFileIsExist = SSHUtils.linuxFileIsExist(shutdownSHDirectory, shutdownSHFileName);
			if(linuxFileIsExist){
				String execCommand = SSHUtils.execCommand("exec "+shutdownSHDirectory+shutdownSHFileName)+"";
				super.getLog().info("shutdown begin..");
				//У��tomcat�Ƿ��Ѿ�ֹͣ
				while(!execCommand.contains("Connection refused")){
					execCommand = SSHUtils.execCommand("exec "+shutdownSHDirectory+shutdownSHFileName)+"";
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				super.getLog().info("shutdown finished");
			}
			//���webapps
			super.getLog().info("���work begin...");
			String clear = SSHUtils.execCommand("rm -rf "+targetFile+"/../work/*");
			super.getLog().info("���work  "+clear);
			clear = SSHUtils.execCommand("rm -rf "+targetFile+"/../temp/*");
			super.getLog().info("���temp  "+clear);
			clear = SSHUtils.execCommand("rm -rf "+targetFile+"/../logs/*");
			super.getLog().info("���logs  "+clear);
			clear = SSHUtils.execCommand("rm -rf "+targetFile+"/../webapps/*");
			super.getLog().info("���webapps  "+clear);
			super.getLog().info("���  finished ");
			String execCommand2 = SSHUtils.execCommand("exec "+targetFile+"/../bin/startup.sh");
			super.getLog().info("startup:::"+execCommand2);
		}
	}
	
	private File[] listWar(File warParentFile){
		File[] listFiles = warParentFile.listFiles(new  FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(pathname.isFile()&&pathname.getName().endsWith(".war")){
					return true;
				}
				return false;
			}
		});
		return listFiles;
	}

	public void setHost(String host) {
		this.host = host;
	}



	public void setPort(String port) {
		this.port = port;
	}



	public void setUser(String user) {
		this.user = user;
	}



	public void setPwd(String pwd) {
		this.pwd = pwd;
	}



	public void setRsyncDirectoryProperties(Properties rsyncDirectoryProperties) {
		this.rsyncDirectoryProperties = rsyncDirectoryProperties;
	}


	public void setRestart(boolean restart) {
		this.restart = restart;
	}

	public void setWarName(String warName) {
		this.warName = warName;
	}

	public void setWarFileProperties(Properties warFileProperties) {
		this.warFileProperties = warFileProperties;
	}

	@Override
	public String toString() {
		return "LoanDeployMojo [host=" + host + ", port=" + port + ", user=" + user + ", pwd=" + pwd + ", restart="
				+ restart + ", warName=" + warName + ", rsyncDirectoryProperties=" + rsyncDirectoryProperties
				+ ", warFileProperties=" + warFileProperties + "]";
	}


	
}
