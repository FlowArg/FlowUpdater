package fr.flowarg.flowupdater.utils;

import java.io.File;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;

public class ForgeHacks
{
	public static void fix(IProgressCallback callback, File dir)
	{
    	callback.step(Step.INTERNAL_FORGE_HACKS);
    	fixAsm(dir);
    	fixGuava(dir);
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
	
	private static void fixGuava(File dir)
	{
    	for(File x : new File(dir, "libraries/com/google/guava/guava/").listFiles())
    	{
    		boolean twenty = false;
    		boolean twentyOne = false;
    		boolean twentyFive = false;
    		for(File y : x.listFiles())
    		{
    			if(y.getName().startsWith("20.0"))
    				twenty = true;
    			if(y.getName().startsWith("21.0"))
    				twentyOne = true;
    			if(y.getName().startsWith("25"))
    				twentyFive = true;
    		}
    			
    		if(twenty && twentyOne && twentyFive)
    		{
        		for(File y : x.listFiles())
        		{
        			if(!y.getName().startsWith("21"))
        				FileUtils.deleteDirectory(y);
        		}
    		}
    	}
	}
}
