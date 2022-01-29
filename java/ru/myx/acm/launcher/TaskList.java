package ru.myx.acm.launcher;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import ru.myx.ae3.binary.Transfer;

/**
 * Title: Scheduler plugin for WSM3 Description: Copyright: Copyright (c) 2001
 * Company:
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */

final class TaskList {
	static final LauncherTask initTask(final ResultSet rs) throws Exception {
		final LauncherTask task = new LauncherTask();
		task.setId( rs.getString( "taskGuid" ) );
		task.setName( rs.getString( "taskName" ) );
		task.setOwner( rs.getString( "taskOwner" ) );
		task.setCommon( "Y".equals( rs.getString( "taskCommon" ) ) );
		task.setLastRun( rs.getTimestamp( "taskLastRun" ) == null
				? 0
				: rs.getTimestamp( "taskLastRun" ).getTime() );
		task.setLastRunLength( rs.getInt( "taskLastRunLength" ) );
		{
			final byte[] bytes = rs.getBytes( "taskLastResult" );
			if (bytes == null || bytes.length == 0) {
				task.setLastResult( "" );
			} else {
				task.setLastResult( Transfer.createBuffer( bytes ).toString( StandardCharsets.UTF_8 ) );
			}
		}
		task.setTaskName( rs.getString( "taskRunner" ) );
		
		{
			final byte[] bytes = rs.getBytes( "taskRunnerSettings" );
			task.setTaskSettings( Transfer.createBuffer( bytes ).toString( StandardCharsets.UTF_8 ) );
		}
		{
			final byte[] bytes = rs.getBytes( "taskRunnerData" );
			task.setTaskData( Transfer.createBuffer( bytes ).toString( StandardCharsets.UTF_8 ) );
		}
		return task;
	}
	
	private final List<LauncherTask>	taskList	= new ArrayList<>( 64 );
	
	private LauncherTask[]				tasks		= null;
	
	final void addTask(final ResultSet rs) throws Exception {
		this.taskList.add( TaskList.initTask( rs ) );
		this.tasks = null;
	}
	
	final LauncherTask[] getTasksArray() {
		if (this.tasks == null) {
			synchronized (this) {
				if (this.tasks == null) {
					this.tasks = new LauncherTask[this.taskList.size()];
					this.tasks = this.taskList.toArray( this.tasks );
				}
			}
		}
		return this.tasks;
	}
}
