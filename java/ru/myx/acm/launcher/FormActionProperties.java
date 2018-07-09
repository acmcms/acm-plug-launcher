/*
 * Created on 27.05.2004
 */
package ru.myx.acm.launcher;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae1.provide.ProvideRunner;
import ru.myx.ae1.provide.TaskRunner;
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
public class FormActionProperties extends AbstractForm<FormActionProperties> {
	private static final ControlCommand<?>	CMD_CREATE	= Control.createCommand( "create", " OK " )
																.setCommandPermission( "publish" )
																.setCommandIcon( "command-save" );
	
	private final ControlFieldset<?>		fieldset;
	
	private final Plugin					plugin;
	
	private final String					key;
	
	private final BaseObject				common;
	
	private final BaseObject				settings;
	
	FormActionProperties(final Plugin plugin,
			final String key,
			final TaskRunner runner,
			final BaseObject common,
			final BaseObject settings) {
		this.plugin = plugin;
		this.key = key;
		this.common = common;
		this.settings = settings;
		
		final ControlFieldset<?> fieldsetCommon = ControlFieldset
				.createFieldset()
				.addField( ControlFieldFactory.createFieldBoolean( "common", MultivariantString.getString( "Task is common",
						Collections.singletonMap( "ru", "Задача общая" ) ), false ) )
				.addField( ControlFieldFactory.createFieldString( "name",
						MultivariantString.getString( "Name", Collections.singletonMap( "ru", "Имя" ) ),
						"" ) )
				.addField( ControlFieldFactory
						.createFieldString( "type",
								MultivariantString.getString( "Task runner",
										Collections.singletonMap( "ru", "Исполнитель задач" ) ),
								"" ).setConstant().setFieldType( "select" ).setFieldVariant( "bigselect" )
						.setAttribute( "lookup", ProvideRunner.getSchedulerTaskRunners( null, null ) ) );
		
		this.fieldset = ControlFieldset.createFieldset().addField( ControlFieldFactory.createFieldMap( "common", //
				MultivariantString.getString( "Execution plan", Collections.singletonMap( "ru", "План выполнения" ) ),
				common )//
				.setFieldVariant( "fieldset" )//
				.setAttribute( "fieldset", fieldsetCommon ) );
		
		final ControlFieldset<?> fieldsetRunner = runner.getFieldset();
		if (fieldsetRunner != null && !fieldsetRunner.isEmpty()) {
			this.fieldset.addField( ControlFieldFactory
					.createFieldMap( "settings",
							MultivariantString.getString( "Additional parameters",
									Collections.singletonMap( "ru", "Параметры для исполнителя" ) ),
							settings ).setFieldVariant( "fieldset" ).setAttribute( "fieldset", fieldsetRunner ) );
		}
		
		this.setAttributeIntern( "id", "action_edit" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Schedule: action properties",
						Collections.singletonMap( "ru", "Планирование: свойства задания" ) ) );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == FormActionProperties.CMD_CREATE) {
			final BaseObject common = this.getData().baseGet( "common", this.common );
			final BaseObject settings = this.getData().baseGet( "settings", this.settings );
			if (this.key == null) {
				this.plugin.addTask( common, settings );
			} else {
				this.plugin.updateTask( this.key, common, settings );
			}
			return null;
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		return Control.createOptionsSingleton( FormActionProperties.CMD_CREATE );
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return this.fieldset;
	}
}
