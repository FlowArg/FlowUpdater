package fr.flowarg.flowupdater.minecraft.versions.download;

public interface IProgressCallback
{
	void init();
	void step(Step step);
	void update(int downloaded, int max);
}
