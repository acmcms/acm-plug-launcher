package ru.myx.acm.launcher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Enumeration;

import ru.myx.ae1.AbstractPluginInstance;
import ru.myx.ae1.BaseRT3;
import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.xml.Xml;

/**
 * Title: Scheduler plugin for WSM3 Description: Copyright: Copyright (c) 2001
 * Company:
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
public final class Plugin extends AbstractPluginInstance {
	private static final byte[]				BYTES_DATA_EMPTY	= "<data/>".getBytes();
	
	private ru.myx.sapi.RuntimeEnvironment	rt;
	
	private String							connectionName		= "default";
	
	private Enumeration<Connection>			connectionSource;
	
	/**
	 * 
	 */
	public Plugin() {
		// empty
	}
	
	void addTask(final BaseObject taskCommon, final BaseObject taskData) {
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn
					.prepareStatement( "INSERT INTO "
							+ "l1Tasks(taskGuid,taskName,taskOwner,taskCommon,taskLastRun,taskLastRunLength,taskLastResult,taskRunner,taskRunnerSettings,taskRunnerData) "
							+ "VALUES (?,?,?,?,?,?,?,?,?,?)" )) {
				ps.setString( 1, Engine.createGuid() );
				ps.setString( 2, Base.getString( taskCommon, "name", "" ) );
				ps.setString( 3, Context.getUserId( Exec.currentProcess() ) );
				ps.setString( 4, Convert.MapEntry.toBoolean( taskCommon, "common", false )
						? "Y"
						: "N" );
				ps.setTimestamp( 5, new Timestamp( 0L ) );
				ps.setInt( 6, 0 );
				ps.setBytes( 7, Transfer.EMPTY_BYTE_ARRAY );
				ps.setString( 8, Base.getString( taskCommon, "type", "" ) );
				final TransferCopier binary = Xml.toXmlBinary( "settings", taskData, false, null, null, 0 );
				final int length = (int) binary.length();
				ps.setBinaryStream( 9, binary.nextCopy().toInputStream(), length );
				ps.setBytes( 10, Plugin.BYTES_DATA_EMPTY );
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			throw new RuntimeException( "Error while adding a task", e );
		}
	}
	
	void deleteTask(final String key) {
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM l1Tasks WHERE taskGuid=?" )) {
				ps.setString( 1, key );
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			throw new RuntimeException( "Error while deleting a task", e );
		}
	}
	
	@Override
	public void destroy() {
		// empty
	}
	
	Connection getConnection() {
		final Connection result = this.connectionSource.nextElement();
		if (result != null) {
			return result;
		}
		final ExecProcess ctx = Exec.currentProcess();
		throw new IllegalArgumentException( "Launcher: no pool available, poolid="
				+ this.connectionName
				+ ", rte="
				+ this.rt
				+ ", rt="
				+ BaseRT3.runtime( ctx )
				+ ", title="
				+ Context.getServer( ctx ).getZoneId() );
	}
	
	LauncherTask getTask(final String key) {
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement( "SELECT * FROM l1Tasks WHERE taskGuid=?",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY )) {
				ps.setMaxRows( 1 );
				ps.setString( 1, key );
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return TaskList.initTask( rs );
					}
				}
			}
		} catch (final Exception e) {
			throw new RuntimeException( "Error while retrieving task, taskGuid=" + key, e );
		}
		return null;
	}
	
	LauncherTask[] getTasks(final boolean all) {
		try (final Connection conn = this.getConnection()) {
			if (all) {
				try (final PreparedStatement ps = conn.prepareStatement( "SELECT * FROM l1Tasks ORDER BY taskName",
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY )) {
					try (final ResultSet rs = ps.executeQuery()) {
						final TaskList tasks = new TaskList();
						while (rs.next()) {
							tasks.addTask( rs );
						}
						return tasks.getTasksArray();
					}
				}
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "SELECT * FROM l1Tasks WHERE taskCommon=? OR taskOwner=? ORDER BY taskName",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY )) {
				ps.setString( 1, "Y" );
				ps.setString( 2, Context.getUserId( Exec.currentProcess() ) );
				try (final ResultSet rs = ps.executeQuery()) {
					final TaskList tasks = new TaskList();
					while (rs.next()) {
						tasks.addTask( rs );
					}
					return tasks.getTasksArray();
				}
			}
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void register() {
		this.rt = BaseRT3.runtime( this.getServer().getRootContext() );
		if (this.connectionName == null) {
			throw new IllegalArgumentException( "'poolid' attribute is undefined!" );
		}
		this.connectionSource = this.getServer().getConnections().get( this.connectionName );
		this.getServer().getControlRoot().bind( new NodeLauncher( this ) );
	}
	
	@Override
	public void setup() {
		final BaseObject info = this.getSettingsProtected();
		this.connectionName = Base.getString( info, "poolid", this.connectionName );
	}
	
	@Override
	public void start() {
		try {
			final AccessManager manager = this.getServer().getAccessManager();
			AccessUser<?> user = manager.getUser( "Scheduler", false );
			if (user == null) {
				user = manager.getUser( "Scheduler", true );
				user.setDescription( "Launcher task runner user" );
				user.setLogin( Engine.createGuid() );
				user.setSystem();
				manager.commitUser( user );
			} else if (!user.isSystem()) {
				user.setSystem();
				manager.commitUser( user );
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return "Launcher plugin for ACM";
	}
	
	void updateTask(final String key, final BaseObject taskCommon, final BaseObject taskData) {
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn
					.prepareStatement( "UPDATE l1Tasks SET taskName=?, taskCommon=?,taskRunner=?,taskRunnerSettings=? WHERE taskGuid=?" )) {
				ps.setString( 1, Base.getString( taskCommon, "name", "" ) );
				ps.setString( 2, //
						Convert.MapEntry.toBoolean( taskCommon, "common", false )
								? "Y" //
								: "N" //
				);
				ps.setString( 3, Base.getString( taskCommon, "type", "" ) );
				final TransferCopier binary = Xml.toXmlBinary( "settings", taskData, false, null, null, 0 );
				final int length = (int) binary.length();
				ps.setBinaryStream( 4, binary.nextCopy().toInputStream(), length );
				ps.setString( 5, key );
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			throw new RuntimeException( "Error while updating a task", e );
		}
	}
	
	void updateTask(
			final String key,
			final long date,
			final long runLength,
			final String result,
			final BaseObject taskInternalData) {
		try (final Connection conn = this.getConnection()) {
			if (taskInternalData != null) {
				try (final PreparedStatement ps = conn
						.prepareStatement( "UPDATE l1Tasks SET taskLastRun=?, taskLastRunLength=?, taskLastResult=?, taskRunnerData=? WHERE taskGuid=?" )) {
					ps.setTimestamp( 1, new Timestamp( date ) );
					ps.setInt( 2, (int) (runLength / 1000) );
					ps.setBytes( 3, result.getBytes( Engine.CHARSET_UTF8 ) );
					final TransferCopier binary = Xml.toXmlBinary( "data", taskInternalData, false, null, null, 0 );
					final int length = (int) binary.length();
					ps.setBinaryStream( 4, binary.nextCopy().toInputStream(), length );
					ps.setString( 5, key );
					ps.executeUpdate();
				}
			} else {
				try (final PreparedStatement ps = conn
						.prepareStatement( "UPDATE l1Tasks SET taskLastRun=?, taskLastRunLength=?, taskLastResult=? WHERE taskGuid=?" )) {
					ps.setTimestamp( 1, new Timestamp( date ) );
					ps.setInt( 2, (int) (runLength / 1000) );
					ps.setBytes( 3, result.getBytes( Engine.CHARSET_UTF8 ) );
					ps.setString( 4, key );
					ps.executeUpdate();
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException( "Error while updating a task", e );
		}
	}
}
