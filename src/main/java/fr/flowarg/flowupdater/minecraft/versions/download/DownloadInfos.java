package fr.flowarg.flowupdater.minecraft.versions.download;

import java.util.List;

import com.google.common.collect.Lists;

import fr.flowarg.flowupdater.minecraft.versions.download.assets.AssetDownloadable;

/**
 * Represent some informations about download status. Used for progress system {@link IProgressCallback}.
 * @author FlowArg
 */
public class DownloadInfos
{
    private final List<Downloadable> libraryDownloadables = Lists.newArrayList();
    private final List<AssetDownloadable> assetDownloadables = Lists.newArrayList();
	private int totalToDownload;
	private int downloaded;
	
	public void init()
	{
		this.totalToDownload = libraryDownloadables.size() + assetDownloadables.size();
		this.downloaded = 0;
	}
	
	public void incrementDownloaded()
	{
		++this.downloaded;
	}
	
	public int getTotalToDownload()
	{
		return this.totalToDownload;
	}
	
	public int getDownloaded()
	{
		return this.downloaded;
	}
	
	public void setTotalToDownload(int totalToDownload)
	{
		this.totalToDownload = totalToDownload;
	}
	
	public void setDownloaded(int downloaded)
	{
		this.downloaded = downloaded;
	}
	
	public List<AssetDownloadable> getAssetDownloadables()
	{
		return this.assetDownloadables;
	}
	
	public List<Downloadable> getLibraryDownloadables()
	{
		return this.libraryDownloadables;
	}
}
