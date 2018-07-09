/*
 * Created on 12.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ru.myx.acm.launcher;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormDeleteConfirmation extends AbstractForm<FormDeleteConfirmation> {
	private static final ControlCommand<?>	DELETE	= Control
															.createCommand( "delete",
																	MultivariantString.getString( "Delete",
																			Collections.singletonMap( "ru", "Удалить" ) ) )
															.setCommandPermission( "delete" )
															.setCommandIcon( "command-delete" );
	
	private final Plugin					scheduler;
	
	private final String					key;
	
	FormDeleteConfirmation(final Plugin scheduler, final String key) {
		this.scheduler = scheduler;
		this.key = key;
		this.setAttributeIntern( "id", "confirmation" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Do you really want to delete this task?",
						Collections.singletonMap( "ru", "Вы действительно хотите удалить это задание?" ) ) );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == FormDeleteConfirmation.DELETE) {
			if (Convert.MapEntry.toBoolean( this.getData(), "confirmation", false )) {
				this.scheduler.deleteTask( this.key );
				return null;
			}
			return "Action cancelled.";
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		return Control.createOptionsSingleton( FormDeleteConfirmation.DELETE );
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return ControlFieldset.createFieldset( "confirmation" ).addField( ControlFieldFactory.createFieldBoolean( "confirmation",
				MultivariantString.getString( "yes, i do.", Collections.singletonMap( "ru", "однозначно" ) ),
				false ) );
	}
	
}
