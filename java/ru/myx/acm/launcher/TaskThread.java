/*
 * Created on 10.12.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.acm.launcher;

import ru.myx.ae1.provide.TaskRunner;
import ru.myx.ae3.Engine;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.report.Report;

final class TaskThread implements Runnable {
	private final LauncherTask	task;
	
	private final long			time;
	
	private Plugin				scheduler;
	
	private TaskRunner			runner;
	
	TaskThread(final LauncherTask task, final long time, final Plugin scheduler, final TaskRunner runner) {
		this.time = time;
		this.task = task;
		this.scheduler = scheduler;
		this.runner = runner;
	}
	
	@Override
	public void run() {
		this.task.lastResult = LauncherTask.RUNNING_STATE;
		
		try {
			if (this.task.settings == null) {
				this.task.settings = new BaseNativeObject();
			}
			if (this.task.data == null) {
				this.task.data = new BaseNativeObject();
			}
			
			try {
				this.runner.run( Exec.currentProcess(), this.task.settings );
				this.task.lastResult = "success";
			} catch (Throwable t) {
				try {
					Report.exception( "SCHEDULER", ("Error while running: " + this.runner.getTitle()), t );
					for (; t.getCause() != null;) {
						t = t.getCause();
					}
					this.task.lastResult = "ERROR: " + t.getMessage();
				} catch (final Throwable tt) {
					tt.printStackTrace();
				}
			} finally {
				try {
					this.scheduler.updateTask( this.task.id,
							Engine.fastTime(),
							(Engine.fastTime() - this.time),
							this.task.lastResult,
							this.task.data );
					
					this.scheduler = null;
					this.runner = null;
				} catch (final Throwable tt) {
					tt.printStackTrace();
				}
			}
		} catch (final Throwable ttt) {
			ttt.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return "shedulerTask: name=" + this.task.name + ", runner=" + this.runner;
	}
}
