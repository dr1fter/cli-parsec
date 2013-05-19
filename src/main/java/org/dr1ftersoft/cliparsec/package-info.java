/**
 * A command line parser framework that makes use of annotations in order to support a declarative way of 
 * describing the respective interface.  
 * 
 * <p>
 * A command line interface is pictured as a (potentially nested) structure.
 * 
 * For example:  
 *   
 *   [global options] command [command options]
 *   
 * To implement this example, declare a type that corresponds to the overall options 'Options'. Annotate fields
 * within this type using the 'GlobalOption' annotation. Supported types for fields are <code>String</code> and
 * <code>boolean</code> (string for options with args, boolean for options without args).
 * 
 * Declare an additional type for each supported (sub-)command. Add an instance of each sub command type to the
 * toplevel options type and annotate it with the 'Command' annotation.
 * 
 */
package org.dr1ftersoft.cliparsec;
