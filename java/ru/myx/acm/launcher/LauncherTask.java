package ru.myx.acm.launcher;

import ru.myx.ae1.provide.TaskRunner;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractBasic;
import ru.myx.ae3.xml.Xml;

/**
 * Title: System Runtime interfaces Description: Copyright: Copyright (c) 2001
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
final class LauncherTask extends AbstractBasic<LauncherTask> {
	
	static final String RUNNING_STATE = "RUNNING";
	
	String id = "";
	
	String name = "unnamed";
	
	String owner = "";
	
	boolean common = false;
	
	long lastRun = 0;
	
	int lastRunLength = 0;
	
	String lastResult = "never run";
	
	String taskName = "";
	
	BaseObject settings;
	
	BaseObject data;
	
	BaseObject fields;
	
	@Override
	public final BaseObject getData() {
		
		if (this.fields == null) {
			final BaseObject fields = new BaseNativeObject()//
					.putAppend("id", this.id)//
					.putAppend("name", this.name)//
					.putAppend("owner", this.owner)//
					.putAppend("common", this.common)//
					.putAppend("lastRun", Base.forDateMillis(this.lastRun))//
					.putAppend("lastRunLength", this.lastRunLength)//
					.putAppend("lastResult", this.lastResult)//
					.putAppend("type", this.taskName)//
					;
			this.fields = fields;
		}
		return this.fields;
	}
	
	@Override
	public String getIcon() {
		
		return null;
	}
	
	@Override
	public final String getKey() {
		
		return this.id;
	}
	
	final Runnable getTaskJob(final long time, final Plugin scheduler, final TaskRunner runner) {
		
		return new TaskThread(this, time, scheduler, runner);
	}
	
	@Override
	public final String getTitle() {
		
		return this.name;
	}
	
	final void setCommon(final boolean common) {
		
		this.common = common;
	}
	
	final void setId(final String id) {
		
		this.id = id;
	}
	
	final void setLastResult(final String lastResult) {
		
		this.lastResult = lastResult;
	}
	
	final void setLastRun(final long lastRun) {
		
		this.lastRun = lastRun;
	}
	
	final void setLastRunLength(final int lastRunLength) {
		
		this.lastRunLength = lastRunLength;
	}
	
	final void setName(final String name) {
		
		this.name = name;
	}
	
	final void setOwner(final String owner) {
		
		this.owner = owner;
	}
	
	final void setTaskData(final String taskData) {
		
		this.data = Xml.toBase("launcherTaskData", taskData, null, null, null);
	}
	
	final void setTaskName(final String taskName) {
		
		this.taskName = taskName;
	}
	
	final void setTaskSettings(final String taskSettings) {
		
		this.settings = Xml.toBase("launcherTaskSettings", taskSettings, null, null, null);
	}
	
	@Override
	public String toString() {
		
		return "[object " + this.baseClass() + "(" + "name=" + this.name + ")]";
	}
}
