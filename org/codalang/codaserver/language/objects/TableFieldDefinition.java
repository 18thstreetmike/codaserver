/*
 * TableFieldDefinition.java
 *
 * Created on June 18, 2007, 11:46 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

import java.util.Calendar;

/**
 *
 * @author michaelarace
 */
public class TableFieldDefinition {
    private String fieldName;
    private String typeName;
    private String refTableName;
    private String displayedAs;
    private boolean arrayFlag;
    private boolean refFlag;
    private boolean identityFlag;
    private boolean nullableFlag = true;
    private long refTableId, typeId, fieldId;
	private String createUserName;
	private String modUserName;
	private long createUserId, modUserId;
    private Calendar createDate, modDate;
    private int defaultVariableId;
    private String defaultValue;
	private boolean builtInFlag = false;

	/** Creates a new instance of TableFieldDefinition */
    public TableFieldDefinition(String fieldName, String typeName, String displayedAs, boolean arrayFlag, boolean refFlag, String refTableName, int defaultVariableId) {
        this.setFieldName(fieldName);
        this.setTypeName(typeName);
        this.setDisplayedAs(displayedAs);
        this.setArrayFlag(arrayFlag);
        this.setRefFlag(refFlag);
        this.setRefTableName(refTableName);
        this.setDefaultVariableId(defaultVariableId);
        this.setDefaultValue(null);
    }
    
    public TableFieldDefinition(String fieldName, String typeName, String displayedAs, boolean arrayFlag, boolean refFlag, String refTableName, String defaultValue) {
        this.setFieldName(fieldName);
        this.setTypeName(typeName);
        this.setDisplayedAs(displayedAs);
        this.setArrayFlag(arrayFlag);
        this.setRefFlag(refFlag);
        this.setRefTableName(refTableName);
        this.setDefaultVariableId(-1);
        this.setDefaultValue(defaultValue);
    }

	public TableFieldDefinition(String fieldName, String typeName, String displayedAs, boolean arrayFlag, boolean refFlag, String refTableName, String defaultValue, boolean builtInFlag) {
        this.setFieldName(fieldName);
        this.setTypeName(typeName);
        this.setDisplayedAs(displayedAs);
        this.setArrayFlag(arrayFlag);
        this.setRefFlag(refFlag);
        this.setRefTableName(refTableName);
        this.setDefaultVariableId(-1);
        this.setDefaultValue(defaultValue);
		this.builtInFlag = builtInFlag;
	}

	public TableFieldDefinition(long fieldId, String fieldName, long typeId, String typeName, String displayedAs, boolean arrayFlag, boolean refFlag, long refTableId, int defaultVariableId, String defaultValue, String createUserName, Calendar createDate, String modUserName, Calendar modDate) {
        this.setFieldId(fieldId);
        this.setFieldName(fieldName);
        this.setTypeName(typeName);
		this.setTypeId(typeId);
		this.setDisplayedAs(displayedAs);
        this.setArrayFlag(arrayFlag);
        this.setRefFlag(refFlag);
        this.setRefTableId(refTableId);
        this.setDefaultVariableId(defaultVariableId);
        this.setDefaultValue(defaultValue);
        this.setCreateUserName(createUserName);
        this.setCreateDate(createDate);
        this.setModUserName(modUserName);
        this.setModDate(modDate);
    }
    
    public TableFieldDefinition(String fieldName) {
        this.setFieldName(fieldName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getRefTableName() {
        return refTableName;
    }

    public void setRefTableName(String refTableName) {
        this.refTableName = refTableName;
    }

    public boolean isArrayFlag() {
        return arrayFlag;
    }

    public void setArrayFlag(boolean arrayFlag) {
        this.arrayFlag = arrayFlag;
    }

    public boolean isRefFlag() {
        return refFlag;
    }

    public void setRefFlag(boolean refFlag) {
        this.refFlag = refFlag;
    }

    public boolean isIdentityFlag() {
        return identityFlag;
    }

    public void setIdentityFlag(boolean identityFlag) {
        this.identityFlag = identityFlag;
    }

    public boolean isNullableFlag() {
        return nullableFlag;
    }

    public void setNullableFlag(boolean nullableFlag) {
        this.nullableFlag = nullableFlag;
    }

    public String getDisplayedAs() {
        return displayedAs;
    }

    public void setDisplayedAs(String displayedAs) {
        this.displayedAs = displayedAs;
    }

    public long getRefTableId() {
        return refTableId;
    }

    public void setRefTableId(long refTableId) {
        this.refTableId = refTableId;
    }

    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    public long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(long createUserId) {
        this.createUserId = createUserId;
    }

    public long getModUserId() {
        return modUserId;
    }

    public void setModUserId(long modUserId) {
        this.modUserId = modUserId;
    }

    public Calendar getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Calendar createDate) {
        this.createDate = createDate;
    }

    public Calendar getModDate() {
        return modDate;
    }

    public void setModDate(Calendar modDate) {
        this.modDate = modDate;
    }

    public long getFieldId() {
        return fieldId;
    }

    public void setFieldId(long fieldId) {
        this.fieldId = fieldId;
    }

    public int getDefaultVariableId() {
        return defaultVariableId;
    }

    public void setDefaultVariableId(int defaultVariableId) {
        this.defaultVariableId = defaultVariableId;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

	public String getCreateUserName() {
		return createUserName;
	}

	public void setCreateUserName(String createUserName) {
		this.createUserName = createUserName;
	}

	public String getModUserName() {
		return modUserName;
	}

	public void setModUserName(String modUserName) {
		this.modUserName = modUserName;
	}

	public boolean isBuiltInFlag() {
		return builtInFlag;
	}

	public void setBuiltInFlag(boolean builtInFlag) {
		this.builtInFlag = builtInFlag;
	}
}
