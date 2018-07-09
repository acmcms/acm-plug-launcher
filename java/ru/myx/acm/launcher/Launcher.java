package ru.myx.acm.launcher;

import java.util.Map;

/**
 * Title: System Runtime interfaces Description: Copyright: Copyright (c) 2001
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */

interface Launcher {
	/**
	 * @param taskCommon
	 * @param taskData
	 */
	void addTask(final Map<?, ?> taskCommon, final Map<?, ?> taskData);
	
	/**
	 * @param key
	 */
	void deleteTask(final String key);
	
	/**
	 * @param key
	 * @return task
	 */
	LauncherTask getTask(String key);
	
	/**
	 * @param all
	 * @return tasks
	 */
	LauncherTask[] getTasks(final boolean all);
	
	/**
	 * @return string
	 */
	String getUserID();
	
	/**
	 * @param key
	 * @param date
	 * @param runLength
	 * @param result
	 * @param taskInternalData
	 */
	void updateTask(
			final String key,
			final long date,
			final long runLength,
			final String result,
			final Map<?, ?> taskInternalData);
	
	/**
	 * @param key
	 * @param taskCommon
	 * @param taskData
	 */
	void updateTask(final String key, final Map<?, ?> taskCommon, final Map<?, ?> taskData);
}
