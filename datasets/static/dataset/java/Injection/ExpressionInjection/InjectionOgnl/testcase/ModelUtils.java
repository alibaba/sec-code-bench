<filename>generator-master/plugins/eclipse/Jet/org.eclipse.jet/src/org/eclipse/jet/model/util/ModelUtils.java<fim_prefix>

/**
 * <copyright>
 *
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id$
 */
        package org.eclipse.jet.model.util;

import java.util.HashMap;
import java.util.List;

import ognl.Ognl;
import ognl.OgnlException;

import org.eclipse.jet.api.ApiEnum;
import org.eclipse.jet.api.Property;
import org.eclipse.jet.model.Data;
import org.eclipse.jet.model.Database;
import org.eclipse.jet.model.Entity;
import org.eclipse.jet.model.Field;
import org.eclipse.jet.model.Group;
import org.eclipse.jet.model.Model;
import org.eclipse.jet.model.Table;
import org.eclipse.jet.model.Userset;
import org.eclipse.jet.model.Xml;

public class ModelUtils
{

    public static org.eclipse.jet.api.Group modelToApi(Model model){
        org.eclipse.jet.api.Group group = new org.eclipse.jet.api.Group();
        if(!(model instanceof Group)){
            return group;
        }
        Group modelGroup = (Group) model;
        String groupName = modelGroup.getName();
        group.setName(groupName);

        group.setDocument(model.getSourceDocument());

        List<Entity> modelEntities = modelGroup.getEntities();
        for (int i = 0; i < modelEntities.size(); i++) {
            Entity modelEntity = modelEntities.get(i);
            String entityName = modelEntity.getName();
            String entityRelation = modelEntity.getRelation();
            String entityFrom = modelEntity.getFrom();
            String entityType = modelEntity.getType();
            org.eclipse.jet.api.Entity entity = new org.eclipse.jet.api.Entity(entityName, entityRelation, entityFrom, entityType);
            entity.setParent(group);
            group.addEntity(entity);
            List<Userset> modelUsersets = modelEntity.getUsersets();
            for (int j = 0; j < modelUsersets.size(); j++) {
                Userset userset = modelUsersets.get(j);
                String propertyName = userset.getName();
                String propertyValue = userset.getText();
                Boolean isNew = true;
                Property property = new Property(propertyName, stringToObject(propertyValue), isNew);
                entity.putProperty(property);
            }
            List<Field> modelFields = modelEntity.getFields();
            for (int j = 0; j < modelFields.size(); j++) {
                Field modelField = modelFields.get(j);
                String fieldId = modelField.getId();
                String fieldName = modelField.getName();
                Boolean fieldIsUsed = null;
                try {
                    fieldIsUsed = Boolean.valueOf(modelField.getIsUsed());
                } catch (Exception e) {

                }
                String fieldType = modelField.getType();
                String fieldDbType = modelField.getDbType();
                String fieldJdbcType = modelField.getJdbcType();
                String fieldFullType = modelField.getFullType();
                Boolean fieldIsNew = modelField.getIsNew();
                String defaultValue = modelField.getDefaultValue();
                org.eclipse.jet.api.Field field = new org.eclipse.jet.api.Field(fieldId, fieldName, fieldIsUsed, fieldType,
                        fieldDbType, fieldJdbcType, fieldFullType, fieldIsNew);
                field.setParent(entity);
                entity.putField(field);

                field.setDefaultValue(defaultValue);

                List<Database> databases = modelField.getDatabases();
                for (int k = 0; k < databases.size(); k++) {
                    Database database = databases.get(k);
                    String propertyName = database.getName();
                    String propertyValue = database.getText();
                    if(propertyName == null){
                        continue;
                    }
                    if(propertyName.equals(ApiEnum.FIELD_IS_NULL.getValue())){
                        field.setIsNull(stringToBoolean(propertyValue));
                    }else if(propertyName.equals(ApiEnum.FIELD_IS_AUTO_INCREMENT.getValue())){
                        field.setIsAutoIncrement(stringToBoolean(propertyValue));
                    }else if(propertyName.equals(ApiEnum.FIELD_SIZE.getValue())){
                        field.setSize(stringToInteger(propertyValue));
                    }else{
                        Property property = new Property(propertyName, stringToObject(propertyValue), false);
                        field.putProperty(property);
                    }
                }

                List<Table> tables = modelField.getTables();
                for (int k = 0; k < tables.size(); k++) {
                    Table table = tables.get(k);
                    String propertyName = table.getName();
                    String propertyValue = table.getText();
                    if(propertyName == null){
                        continue;
                    }
                    if(propertyName.equals(ApiEnum.FIELD_IS_PK.getValue())){
                        field.setIsPk(stringToBoolean(propertyValue));
                    }else if(propertyName.equals(ApiEnum.FIELD_IS_FK.getValue())){
                        field.setIsFk(stringToBoolean(propertyValue));
                    }else if(propertyName.equals(ApiEnum.FIELD_FK_ENTITY_NAME.getValue())){
                        field.setFkEntityName(propertyValue);
                    }else if(propertyName.equals(ApiEnum.FIELD_FK_FIELD_NAME.getValue())){
                        field.setFkFieldName(propertyValue);
                    }else{
                        Property property = new Property(propertyName, stringToObject(propertyValue), false);
                        field.putProperty(property);
                    }
                }

                List<Userset> usersets = modelField.getUsersets();
                for (int k = 0; k < usersets.size(); k++) {
                    Userset userset = usersets.get(k);
                    String propertyName = userset.getName();
                    String propertyValue = userset.getText();
                    Property property = new Property(propertyName, stringToObject(propertyValue), true);
                    field.putProperty(property);
                }

                List<Data> datas = modelField.getDatas();
                for (int k = 0; k < datas.size(); k++) {
                    Data modelData = datas.get(k);
                    String modelName = modelData.getName();
                    String modelValue = modelData.getText();
                    org.eclipse.jet.api.Data data = new org.eclipse.jet.api.Data(modelName, stringToObject(modelValue));
                    field.putData(data);
                }
            }
        }

        List<Xml> modelXmls = modelGroup.getXmls();
        for (int i = 0; i < modelXmls.size(); i++) {
            Xml modelXml = modelXmls.get(i);
            String xmlName = modelXml.getName();
            String xmlRelation = modelXml.getRelation();
            String xmlPath = modelXml.getPath();
            org.eclipse.jet.api.Xml xml = new org.eclipse.jet.api.Xml(xmlName, xmlRelation, xmlPath);
            xml.setParent(group);
            group.addXml(xml);
        }

        return group;
    }

    private static Boolean stringToBoolean(String str){
        if(str == null){
            return null;
        }
        if(str.equals(""true"")){
            return true;
        }else if(str.equals(""false"")){
            return false;
        }else{
            return null;
        }
    }

    private static Integer stringToInteger(String str){
        Integer integer = null;
        try
        {
            integer = Integer.valueOf(str);
        }catch (NumberFormatException e){

        }
        return integer;
    }

    public static Object stringToObject(String str){
        if(str == null){
            return str;
        }
        Object object = null;
        try {
<fim_suffix>
        } catch (OgnlException e) {
        }
        if(object != null){
            return object;
        }
        return str;
    }

}
<fim_middle>