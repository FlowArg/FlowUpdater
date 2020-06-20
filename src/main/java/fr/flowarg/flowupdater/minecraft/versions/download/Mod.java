package fr.flowarg.flowupdater.minecraft.versions.download;

public class Mod
{
	private String name;
	private String sha1;
	private int size;
	private String downloadURL;
	
	public Mod(String name, String sha1, int size, String downloadURL)
	{
		this.name = name;
		this.sha1 = sha1;
		this.size = size;
		this.downloadURL =  downloadURL;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String getSha1()
	{
		return this.sha1;
	}
	
	public int getSize()
	{
		return this.size;
	}
	
	public String getDownloadURL()
	{
		return this.downloadURL;
	}
}
