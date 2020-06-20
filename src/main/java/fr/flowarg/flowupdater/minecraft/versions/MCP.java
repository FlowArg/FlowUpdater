package fr.flowarg.flowupdater.minecraft.versions;

public class MCP
{
	private String clientDownloadURL;
	private String name;
	private String clientSha1;
	private int clientSize;
	private int serverSize;
	private String author;
	private String serverDownloadURL;
	private String serverSha1;
	
	public MCP(String clientDownloadURL, String name, String clientSha1, String author, String serverDownloadURL, String serverSha1, int clientSize, int serverSize)
	{
		this.clientDownloadURL = clientDownloadURL;
		this.name = name;
		this.clientSha1 = clientSha1;
		this.author = author;
		this.serverDownloadURL = serverDownloadURL;
		this.serverSha1 = serverSha1;
		this.clientSize = clientSize;
		this.serverSize = serverSize;
	}
	
	public String getAuthor()
	{
		return this.author;
	}
	
	public String getClientDownloadURL()
	{
		return this.clientDownloadURL;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String getClientSha1()
	{
		return this.clientSha1;
	}
	
	public String getServerDownloadURL()
	{
		return this.serverDownloadURL;
	}
	
	public String getServerSha1()
	{
		return this.serverSha1;
	}
	
	public int getClientSize()
	{
		return this.clientSize;
	}
	
	public int getServerSize()
	{
		return this.serverSize;
	}
}
