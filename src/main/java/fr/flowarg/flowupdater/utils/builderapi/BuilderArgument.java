package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.VanillaVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder API
 * 
 * @author flow
 * @version 1.4
 * 
 * Used for {@link FlowUpdater} & {@link VanillaVersion}
 * @param <T> Object Argument
 * 
 * Represent an argument for a Builder implementation.
 */
public class BuilderArgument<T>
{
	private final String objectName;
	private T badObject = null;
	private T object = null;
	private boolean required;
	private final List<BuilderArgument<?>> requireds = new ArrayList<>();
	
	public BuilderArgument(String objectName, T initialValue)
	{
		this.objectName = objectName;
		this.object = initialValue;
	}
	
	public BuilderArgument(String objectName)
	{
		this.objectName = objectName;
	}
	
	public BuilderArgument(String objectName, T initialValue, T badObject)
	{
		this.objectName = objectName;
		this.object = initialValue;
		this.badObject = badObject;
	}
	
	public BuilderArgument(T badObject, String objectName)
	{
		this.objectName = objectName;
		this.badObject = badObject;
	}
	
	public T get() throws BuilderException
	{
		if(this.required)
		{
			if(this.object == null || this.object == this.badObject)
				throw new BuilderException("Argument" + this.objectName + " is null/a bad object !");
			else return this.object;
		}
		else
		{
			this.requireds.forEach(arg -> {
				try
				{
					if((arg.get() == null || arg.get() == arg.badObject()) && this.object != null)
						throw new BuilderException(arg.getObjectName() + " cannot be null/a bad object if you're using " + this.objectName + " argument !");
				}
				catch (BuilderException e)
				{
					e.printStackTrace();
				}
			});
			return this.object;
		}
	}
	
	public void set(T object)
	{
		this.object = object;
	}
	
	public BuilderArgument<T> require(BuilderArgument<?>... requireds)
	{
		this.requireds.addAll(Arrays.asList(requireds));
		return this;
	}
	
	public BuilderArgument<T> required()
	{
		this.required = true;
		return this;
	}
	
	public BuilderArgument<T> optional()
	{
		this.required = false;
		return this;
	}
	
	public String getObjectName()
	{
		return this.objectName;
	}
	
	public T badObject()
	{
		return this.badObject;
	}
}
