/*
 *   Copyright (c) 2014 Program Analysis Group, Georgia Tech
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package edu.gatech.sqltutor.rules.symbolic.tokens;

import edu.gatech.sqltutor.rules.symbolic.SymbolicType;

/**
 * This represents a reference to a table-entity 
 * that will be resolved based on context.
 */
public class TableEntityRefToken extends AbstractSymbolicToken implements ISymbolicToken {
	private TableEntityToken tableEntity;
	private boolean needsId = false;

	public TableEntityRefToken(TableEntityRefToken toCopy) {
		super(toCopy);
		this.tableEntity = toCopy.tableEntity;
		this.needsId = toCopy.needsId;
	}
	
	public TableEntityRefToken(TableEntityToken tableEntity) {
		super(tableEntity.getPartOfSpeech());
		this.tableEntity = tableEntity;
	}
	
	public TableEntityToken getTableEntity() {
		return tableEntity;
	}
	
	public void setTableEntity(TableEntityToken tableEntity) {
		this.tableEntity = tableEntity;
	}
	
	public void setNeedsId(boolean needsId) {
		this.needsId = needsId;
	}
	
	public boolean getNeedsId() {
		return needsId;
	}
	
	@Override
	public SymbolicType getType() {
		return SymbolicType.TABLE_ENTITY_REF;
	}
	
	@Override
	protected StringBuilder addPropertiesString(StringBuilder b) {
		b.append("tableEntity=").append(tableEntity)
			.append(", needsId=").append(needsId);
		return b;
	}
}
