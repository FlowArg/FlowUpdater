package fr.flowarg.flowupdater.utils;

public class BuilderArgument<T>
{
	private T object = null;
	private boolean required;
	
	public BuilderArgument(T initialValue)
	{
		this.object = initialValue;
	}
	
	public BuilderArgument() {}
	
	public T get() throws BuilderArgumentException
	{
		if(this.required)
		{
			if(this.object == null)
				throw new BuilderArgumentException("Current argument is null !");
			else return this.object;
		}
		else return this.object;
	}
	
	public void set(T object)
	{
		this.object = object;
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
}
