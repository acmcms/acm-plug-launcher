package ru.myx.acm.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.control.AbstractNode;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae1.provide.ProvideRunner;
import ru.myx.ae1.provide.TaskRunner;
import ru.myx.ae3.Engine;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlField;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.report.Report;

/**
 * Title: Base Implementations Description: Copyright: Copyright (c) 2001
 * Company: -= MyX =-
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
class NodeLauncher extends AbstractNode {
	
	private static final ControlFieldset<?> LISTING_DEFINITION = ControlFieldset.createFieldset().addFields(new ControlField[]{
			ControlFieldFactory.createFieldString("name", MultivariantString.getString("Name", Collections.singletonMap("ru", "Имя")), ""),
			ControlFieldFactory.createFieldOwner("owner", MultivariantString.getString("Owner", Collections.singletonMap("ru", "Владелец"))),
			ControlFieldFactory.createFieldBoolean("common", MultivariantString.getString("Common", Collections.singletonMap("ru", "Общая")), false),
			ControlFieldFactory.createFieldDate("lastRun", MultivariantString.getString("Last run", Collections.singletonMap("ru", "Последний запуск")), 0L),
			ControlFieldFactory.createFieldString("lastResult", MultivariantString.getString("Last result", Collections.singletonMap("ru", "Результат")), "").setConstant(),
	});
	
	private static final ControlCommand<?> CMD_CREATE = Control.createCommand("create", MultivariantString.getString("Create...", Collections.singletonMap("ru", "Создать...")))
			.setCommandPermission("create").setCommandIcon("command-create");
			
	private static final Object TITLE = MultivariantString.getString("Launch pad", Collections.singletonMap("ru", "Планировщик"));
	
	final Plugin scheduler;
	
	NodeLauncher(final Plugin scheduler) {
		this.scheduler = scheduler;
	}
	
	@Override
	public AccessPermissions getCommandPermissions() {
		
		return Access.createPermissionsLocal().addPermission("execute", MultivariantString.getString("Run tasks", Collections.singletonMap("ru", "Запускать задания")))
				.addPermission("view", MultivariantString.getString("View task properties", Collections.singletonMap("ru", "Просматривать свойства задания")))
				.addPermission("create", MultivariantString.getString("Create tasks", Collections.singletonMap("ru", "Создавать задания")))
				.addPermission("modify", MultivariantString.getString("Modify tasks", Collections.singletonMap("ru", "Изменять задания")))
				.addPermission("delete", MultivariantString.getString("Delete tasks", Collections.singletonMap("ru", "Удалять задания")));
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		
		if (command == NodeLauncher.CMD_CREATE) {
			return new FormCreateAction(this.scheduler);
		}
		if ("run".equals(command.getKey())) {
			this.launch(Base.getString(command.getAttributes(), "key", ""));
			try {
				Thread.sleep(500L);
			} catch (final InterruptedException e) {
				// ignore
			}
			return null;
		}
		if ("edit".equals(command.getKey())) {
			final String key = Base.getString(command.getAttributes(), "key", "");
			final LauncherTask task = this.scheduler.getTask(key);
			if (task == null) {
				throw new IllegalArgumentException("Task " + key + " was not found!");
			}
			final TaskRunner runner = ProvideRunner.forName(task.taskName);
			if (runner == null) {
				throw new IllegalArgumentException("Runner " + task.taskName + " is unknown!");
			}
			final BaseObject common = task.getData();
			final BaseObject settings = task.settings;
			return new FormActionProperties(this.scheduler, key, runner, common, settings);
		}
		if ("delete".equals(command.getKey())) {
			return new FormDeleteConfirmation(this.scheduler, Base.getString(command.getAttributes(), "key", ""));
		}
		if ("run_multi".equals(command.getKey())) {
			final BaseArray keys = Convert.MapEntry.toCollection(command.getAttributes(), "keys", null);
			if (keys != null) {
				final int length = keys.length();
				for (int i = 0; i < length; ++i) {
					this.launch(keys.baseGet(i, BaseObject.UNDEFINED).baseToJavaString());
				}
			}
			try {
				Thread.sleep(500L);
			} catch (final InterruptedException e) {
				// ignore
			}
			return null;
		}
		if ("delete_multi".equals(command.getKey())) {
			final BaseArray keys = Convert.MapEntry.toCollection(command.getAttributes(), "keys", null);
			if (keys != null && !keys.isEmpty()) {
				if (keys.length() == 1) {
					return new FormDeleteConfirmation(this.scheduler, keys.baseGet(0, BaseObject.UNDEFINED).baseToJavaString());
				}
				return new FormDeleteConfirmationMultiple(this.scheduler, keys);
			}
			return null;
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		return Control.createOptionsSingleton(NodeLauncher.CMD_CREATE);
	}
	
	@Override
	public ControlCommandset getContentCommands(final String key) {
		
		final ControlCommandset result = Control.createOptions();
		result.add(
				Control.createCommand("run", MultivariantString.getString("Run", Collections.singletonMap("ru", "Запустить"))).setCommandPermission("execute")
						.setCommandIcon("command-run").setAttribute("key", key));
		result.add(
				Control.createCommand("edit", MultivariantString.getString("Properties", Collections.singletonMap("ru", "Свойства"))).setCommandPermission("view")
						.setCommandIcon("command-edit").setAttribute("key", key));
		result.add(
				Control.createCommand("delete", MultivariantString.getString("Delete", Collections.singletonMap("ru", "Удалить"))).setCommandPermission("delete")
						.setCommandIcon("command-delete").setAttribute("key", key));
		return result;
	}
	
	@Override
	public ControlFieldset<?> getContentFieldset() {
		
		return NodeLauncher.LISTING_DEFINITION;
	}
	
	@Override
	public ControlCommandset getContentMultipleCommands(final BaseArray keys) {
		
		final ControlCommandset result = Control.createOptions();
		result.add(
				Control.createCommand("run_multi", MultivariantString.getString("Run", Collections.singletonMap("ru", "Запустить"))).setCommandPermission("execute")
						.setCommandIcon("command-run").setAttribute("keys", keys));
		result.add(
				Control.createCommand("delete_multi", MultivariantString.getString("Delete", Collections.singletonMap("ru", "Удалить"))).setCommandPermission("delete")
						.setCommandIcon("command-delete").setAttribute("keys", keys));
		return result;
	}
	
	@Override
	public List<ControlBasic<?>> getContents() {
		
		final LauncherTask[] tasks = this.scheduler.getTasks(false);
		final List<ControlBasic<?>> result = new ArrayList<>();
		for (final LauncherTask task : tasks) {
			result.add(Control.createBasic(task.id, task.name, task.getData()));
		}
		return result;
	}
	
	@Override
	public String getIcon() {
		
		return "container-scheduler";
	}
	
	@Override
	public String getKey() {
		
		return "LauNCheR";
	}
	
	@Override
	public String getTitle() {
		
		return NodeLauncher.TITLE.toString();
	}
	
	private void launch(final String key) {
		
		final long time = Engine.fastTime();
		final LauncherTask task = this.scheduler.getTask(key);
		if (task == null) {
			throw new IllegalArgumentException("Task is unaccessable, taskId=" + key);
		}
		final TaskRunner runner = ProvideRunner.forName(task.taskName);
		if (runner == null) {
			throw new IllegalArgumentException("Task runner is unaccessable, taskName=" + task.taskName);
		}
		final ExecProcess process = Exec.createProcess(null, "scheduler task context");
		Context.replaceQuery(process, null); // derive context
		final Context context = Context.getContext(process);
		context.replaceUserId("Scheduler");
		context.replaceSessionId(Engine.createGuid());
		final Runnable scheduled = task.getTaskJob(time, this.scheduler, runner);
		this.scheduler.updateTask(key, time, 0, LauncherTask.RUNNING_STATE, null);
		Act.launch(process, scheduled);
		Report.started("RT3/SCHEDULER", "forced=true&id=" + task.id + "&task=" + runner.getTitle() + "&title=" + task.name);
	}
}
