/*
 * GroovyClassGenerator.java
 *
 * Created on September 17, 2007, 3:05 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import org.codalang.codaserver.language.objects.ProcedureParameter;
import org.codalang.codaserver.language.objects.TableFieldDefinition;

import java.io.IOException;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class GroovyClassGenerator {
    
    public static final String[] reservedWords = new String[] {"abstract","do","if","package","synchronized","boolean","double","implements","private","this","break","else","import","protected","throw","byte","extends","instanceof","public","throws","case","false","int","return","transient","catch","final","interface","short","true","char","finally","long","static","try","class","float","native","strictfp","void","const","for","new","super","volatile","continue", "goto", "null", "switch", "while","default","assert"};
    
    /** Creates a new instance of GroovyClassGenerator */
    public GroovyClassGenerator() {
    }
    
    public static String getTypeClass(String typeName, String validationMask, String saveMask) throws IOException {
        String groovy = 
            "package org.codalang.codaserver.language.types.user\n" +
            "import org.codalang.codaserver.language.types.BaseCodaType\n" +
            "class " + CodaServer.camelCapitalize(typeName, true) + " extends BaseCodaType {\n" +
            "String getValidationString() {\n" +
            "   return '" + validationMask.substring(1, validationMask.length() - 2).replace("\\", "\\\\") + "'\n" +
            "}\n" +
            "String getSaveString() {\n" +
            "   return '" + saveMask.substring(1, saveMask.length() - 2).replace("\\", "\\\\") + "'\n" +
            "}\n" +
            "}\n";
        /*
		GroovyClassLoader loader = new GroovyClassLoader();
        Class groovyClass = loader.parseClass(groovy);

		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArray);
        out.writeObject(groovyClass);

		return byteArray.toByteArray();

		InputStream inStream = groovyClass.getResourceAsStream("/" + CodaServer.capitalize(typeName));
		byte[] retval = new byte [0];
		try {
			retval = new byte [inStream.available()];
		} catch(Exception ex) {
			int i = 1;
		}
		inStream.read(retval);
		*/
		return groovy;
	}
    
    public static String getTableClass(String tableName, Vector<TableFieldDefinition> fields) throws IOException {
        String groovy = 
            "package org.codalang.codaserver.language.tables\n" +
            "import org.codalang.codaserver.language.types.*\n" +
            "class " + CodaServer.camelCapitalize(tableName, true) + " implements BaseCodaTable {\n";
        for(TableFieldDefinition field : fields) {
            groovy += CodaServer.camelCapitalize(field.getTypeName(), true) + " " + CodaServer.camelCapitalize(field.getFieldName(), false) + "\n";
        }    
        groovy += 
                "void setFields(Hashtable fields) {\n" +
				"if (fields != null) {\n";
        for(TableFieldDefinition field : fields) {
            groovy += "this." + CodaServer.camelCapitalize(field.getFieldName(), false) + " = fields[\""+ field.getFieldName().toLowerCase() +"\"]\n";
        }    
		groovy += "}\n";    
		groovy += "}\n";
        groovy += "}\n";
		/*
		GroovyClassLoader loader = new GroovyClassLoader();
        Class groovyClass = loader.parseClass(groovy);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArray);
        out.writeObject(groovyClass);
        */
        return groovy;
    }
    
    public static String getProcedureClass (String procedureName, Vector<ProcedureParameter> parameters, String procedureBody) throws IOException {
        String declarations = "";
        for (ProcedureParameter parameter : parameters) {
            declarations += CodaServer.camelCapitalize(parameter.getParameterType(), true) + (parameter.isArrayFlag() ? "[] " : " " )+ CodaServer.camelCapitalize(parameter.getParameterName(), false) + " = parameters[\""+parameter.getParameterName().toUpperCase()+"\"];\n";
        }
        String groovy =
            "package org.codalang.codaserver.language.procedures\n" +
            "import org.codalang.codaserver.language.types.*\n" +
            "import org.codalang.codaserver.language.types.user.*\n" +
            "import org.codalang.codaserver.language.tables.*\n" +
            "import org.codalang.codaserver.CodaException\n" + 
                
            "class " + CodaServer.camelCapitalize(procedureName, true) + " implements BaseCodaProcedure {\n" +
            "Object execute(Database database, Hashtable parameters) throws CodaException {\n" +
            declarations +
            procedureBody +
            "   \n" +
            "}\n" +
            "}\n";
		/*
		GroovyClassLoader loader = new GroovyClassLoader();
        Class groovyClass = loader.parseClass(groovy);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArray);
        out.writeObject(groovyClass);
        */
        return groovy;
    }
    
    public static String getTriggerClass (String tableName, String operation, String beforeAfterString, String triggerBody) throws IOException {
        String groovy =
            "package org.codalang.codaserver.language.triggers\n" +
            "import org.codalang.codaserver.language.types.*\n" +
            "import org.codalang.codaserver.language.types.user.*\n" +
            "import org.codalang.codaserver.language.tables.*\n" +
            "import org.codalang.codaserver.CodaException\n" +
                
            "class " + CodaServer.camelCapitalize(tableName, true) + CodaServer.camelCapitalize(beforeAfterString, true) + CodaServer.camelCapitalize(operation, true) + " implements BaseCodaTrigger {\n" +
            "void fire(Database database, BaseCodaTable next, BaseCodaTable prev) throws CodaException {\n" +
            triggerBody +
            "   \n" +
            "}\n" +
            "}\n";
		/*
		GroovyClassLoader loader = new GroovyClassLoader();
        Class groovyClass = loader.parseClass(groovy);
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArray);
        out.writeObject(groovyClass);
        */
        return groovy;
    }
}
