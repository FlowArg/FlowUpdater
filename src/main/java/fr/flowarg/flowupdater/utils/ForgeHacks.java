package fr.flowarg.flowupdater.utils;

import java.io.File;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;

public class ForgeHacks
{
	public static void fix(IProgressCallback callback, File dir, String version)
	{
    	callback.step(Step.INTERNAL_FORGE_HACKS);
    	fixAsm(dir);
    	fixGuava(dir, version);
	}
	
	private static void fixAsm(File dir)
	{
    	for(File x : new File(dir, "libraries/org/ow2/asm/").listFiles())
    	{
    		if(x.listFiles() != null)
    		{
    			boolean sevenPresent = false;
    			boolean sixPresent = false;
    			for(File y : x.listFiles())
    			{
    				if(y.getName().startsWith("7"))
    					sevenPresent = true;
    				if(y.getName().startsWith("6"))
    					sixPresent = true;
    			}
    			
    			if(sevenPresent && sixPresent)
    			{
        			for(File y : x.listFiles())
        			{
        				if(y.getName().startsWith("6"))
        					FileUtils.deleteDirectory(y);
        			}
    			}
    		}
    	}
	}
	
	private static void fixGuava(File dir, String version)
	{
		final String[] concerned = {"1.13", "1.14", "1.15", "1.16"};
		boolean flag = false;
		for(String str : concerned)
		{
			if(version.contains(str))
			{
				flag = true;
				break;
			}
		}
		
		if(flag)
		{
	    	for(File x : new File(dir, "libraries/com/google/guava/guava/").listFiles())
	    	{
	    		boolean twenty = false;
	    		boolean twentyOne = false;
	    		boolean twentyFive = false;
	            if(x.getName().startsWith("20"))
	                twenty = true;
	            if(x.getName().startsWith("21"))
	                twentyOne = true;
	            if(x.getName().startsWith("25"))
	                twentyFive = true;
	    		
	    		if(twenty && twentyOne && twentyFive)
	    		{
	                if(!x.getName().startsWith("21"))
	                    FileUtils.deleteDirectory(x);
	    		}
	    	}
		}
	}
}
