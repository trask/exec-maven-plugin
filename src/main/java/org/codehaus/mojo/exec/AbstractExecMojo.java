package org.codehaus.mojo.exec;

/*
 * Copyright 2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * This class is used for unifying functionality between the 2 mojo exec plugins ('java' and 'exec').
 * It handles parsing the arguments and adding source/test folders.
 * 
 * @author Philippe Jacot (PJA)
 * @author Jerome Lacoste
 */
public abstract class AbstractExecMojo extends AbstractMojo
{
    /**
     * The enclosing project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * This folder is added to the list of those folders
     * containing source to be compiled. Use this if your
     * plugin generates source code.
     *
     * @parameter expression="${sourceRoot}"
     */
    private File sourceRoot;

    /**
     * This folder is added to the list of those folders
     * containing source to be compiled for testing. Use this if your
     * plugin generates test source code.
     *
     * @parameter expression="${testSourceRoot}"
     */
    private File testSourceRoot;

    /**
     * Arguments for the executed program
     * 
     * @parameter expression="${exec.args}"
     */
    private String commandlineArgs;
    
    /**
     * Defines the scope of the classpath passed to the plugin. Set to compile,test,runtime or system depending
     * on your needs.
     * Since 1.1.2, the default value is 'runtime' instead of 'compile'.
     * @parameter expression="${exec.classpathScope}" default-value="runtime"
     */
    protected String classpathScope;

	/**
	 * Skip the execution.
	 *
	 * @parameter expression="${skip}" default-value="false"
	 * @since 1.0.1
	 */
	private boolean skip;

    /** @see #parseCommandlineArgs */
    private static final char PARAMETER_DELIMITER = ' ';

    /** @see #parseCommandlineArgs */
    private static final char STRING_WRAPPER = '"';

    /** @see #parseCommandlineArgs */
    private static final char ESCAPE_CHAR = '\\';

    /**
     * Collects the project artifacts in the specified List and the project specific classpath 
     * (build output and build test output) Files in the specified List, depending on the plugin classpathScope value.
     * @param artifacts the list where to collect the scope specific artifacts
     * @param theClasspathFiles the list where to collect the scope specific output directories
     * @throws NullPointerException if at least one of the parameter is null
     */
    protected void collectProjectArtifactsAndClasspath( List artifacts, List theClasspathFiles )
    {
        
        if ( "compile".equals( classpathScope ) )
        {
            artifacts.addAll( project.getCompileArtifacts() );
            theClasspathFiles.add( new File( project.getBuild().getOutputDirectory() ) );
        }
        else if ( "test".equals( classpathScope ) )
        {
            artifacts.addAll( project.getTestArtifacts() );
            theClasspathFiles.add( new File( project.getBuild().getTestOutputDirectory() ) );
            theClasspathFiles.add( new File( project.getBuild().getOutputDirectory() ) );
        }
        else if ( "runtime".equals( classpathScope ) )
        {
            artifacts.addAll( project.getRuntimeArtifacts() );
            theClasspathFiles.add( new File( project.getBuild().getOutputDirectory() ) );
        }
        else if ( "system".equals( classpathScope ) )
        {
            artifacts.addAll( project.getSystemArtifacts() );
        }
        else
        {
            throw new IllegalStateException( "Invalid classpath scope: " + classpathScope );
        }

        getLog().debug( "Collected project artifacts " + artifacts );
        getLog().debug( "Collected project classpath " + theClasspathFiles );
    }

    /**
     * Parses the argument string given by the user. Strings are recognized as everything between STRING_WRAPPER.
     * PARAMETER_DELIMITER is ignored inside a string. STRING_WRAPPER and PARAMETER_DELIMITER can be escaped using
     * ESCAPE_CHAR.
     * 
     * @return Array of String representing the arguments
     * @throws MojoExecutionException
     *             for wrong formatted arguments
     */
    protected String[] parseCommandlineArgs() throws MojoExecutionException
    {
        if ( commandlineArgs == null )
        {
            return null;
        }
        else {
            try
            {
                return CommandLineUtils.translateCommandline( commandlineArgs );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( e.getMessage() );
            }
        }
    }

    /**
     * @return true of the mojo has command line arguments
     */
    protected boolean hasCommandlineArgs()
    {
        return ( commandlineArgs != null );
    }

    /**
     * Register compile and compile tests source roots if necessary
     */
    protected void registerSourceRoots()
    {
        if ( sourceRoot != null )
        {
            getLog().info( "Registering compile source root " + sourceRoot );
            project.addCompileSourceRoot( sourceRoot.toString() );
        }

        if ( testSourceRoot != null )
        {
            getLog().info( "Registering compile test source root " + testSourceRoot );
            project.addTestCompileSourceRoot( testSourceRoot.toString() );
        }
    }

    public boolean isSkip()
    {
        return skip;
    }
}
