/*
 * Created on 27.05.2004
 */
package ru.myx.acm.launcher;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae1.provide.ProvideRunner;
import ru.myx.ae1.provide.TaskRunner;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormCreateAction extends AbstractForm<FormCreateAction> {
	private static final ControlCommand<?>	CMD_CREATE	= Control.createCommand( "create", " OK " )
																.setCommandPermission( "publish" )
																.setCommandIcon( "command-save" );
	
	private final Plugin					plugin;
	
	FormCreateAction(final Plugin plugin) {
		this.plugin = plugin;
		
		this.setAttributeIntern( "id", "scheduler_add" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Schedule: add an action",
						Collections.singletonMap( "ru", "Планирование: добавление задания" ) ) );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == FormCreateAction.CMD_CREATE) {
			final String scheduleActor = Base.getString( this.getData(), "type", "" );
			final TaskRunner runner = ProvideRunner.forName( scheduleActor );
			if (runner == null) {
				throw new IllegalArgumentException( "Runner " + scheduleActor + " is unknown!" );
			}
			final ControlFieldset<?> runnerFieldset = runner.getFieldset();
			if (runnerFieldset == null || runnerFieldset.isEmpty()) {
				this.plugin.addTask( this.getData(), new BaseNativeObject() );
				return null;
			}
			return new FormActionProperties( this.plugin, null, runner, this.getData(), BaseObject.UNDEFINED );
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		return Control.createOptionsSingleton( FormCreateAction.CMD_CREATE );
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return ControlFieldset
				.createFieldset( "choose_type" )
				.addField( ControlFieldFactory.createFieldBoolean( "common",
						MultivariantString.getString( "Task is common/public (for every user)",
								Collections.singletonMap( "ru", "Задача общая (для каждого пользователя)" ) ),
						false ) )
				.addField( ControlFieldFactory.createFieldString( "name",
						MultivariantString.getString( "Task name", Collections.singletonMap( "ru", "Имя задачи" ) ),
						"",
						1,
						255 ) )
				.addField( ControlFieldFactory
						.createFieldString( "type",
								MultivariantString.getString( "Task runner",
										Collections.singletonMap( "ru", "Исполнитель задач" ) ),
								"",
								1,
								128 ).setFieldType( "select" ).setFieldVariant( "bigselect" )
						.setAttribute( "lookup", ProvideRunner.getSchedulerTaskRunners( null, null ) ) );
	}
}
