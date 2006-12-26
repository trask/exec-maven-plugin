package org.codehaus.mojo.exec;

/*
 * Copyright 2005-2006 The Codehaus.
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.PrintStream;

/**
 * @author Jerome Lacoste <jerome@coffeebreaks.org>
 * @version $Id$
 */
public class ExecJavaMojoTest
    extends AbstractMojoTestCase
{
    /**
     * Check that a simple execution with no arguments and no system properties produces the expected result
     * <p/>
     * we load the config from a pom file and fill up the MavenProject property ourselves
     */
    public void testSimpleRun()
        throws Exception
    {
        File pom = new File( getBasedir(), "src/test/projects/project4/pom.xml" );

        String output = execute( pom, "java" );

        assertEquals( "Hello" + System.getProperty( "line.separator" ), output );
    }

    /**
     * MEXEC-10 Check that an execution with no arguments and an system property with no value
     * produces the expected result
     * <p/>
     * we load the config from a pom file and fill up the MavenProject property ourselves
     */
    public void testEmptySystemProperty()
        throws Exception
    {
        File pom = new File( getBasedir(), "src/test/projects/project5/pom.xml" );

        assertNull( "System property not yet created",
                     System.getProperty( "project5.property.with.no.value" ) );

        execute( pom, "java" );

        assertEquals( "System property now empty",
                       "",
                       System.getProperty( "project5.property.with.no.value" ) );
    }

    /**
     * For cases where the Java code spawns Threads and main returns soon.
     * See <a href="http://jira.codehaus.org/browse/MEXEC-6">MEXEC-6</a>.
     */
    public void testWaitNoDaemonThreads()
        throws Exception
    {
        File pom = new File( getBasedir(), "src/test/projects/project7/pom.xml" );

        String output = execute( pom, "java" );

        assertEquals( MainWithThreads.SUCCESS, output.trim() );
    }

    /**
     * Test the commandline parsing facilities of the {@link AbstractExecMojo} class
     */
    public void testRunWithArgs() throws Exception
    {

        String resultString = execute( new File( getBasedir(), "src/test/projects/project8/pom.xml" ), "java" );

        String LS = System.getProperty("line.separator");
        String expectedResult = "Hello" + LS + "Arg1" + LS +"Arg2a Arg2b" + LS;
        assertEquals( expectedResult, resultString );
    }

    /**
     * @return output from System.out during mojo execution
     */
    private String execute( File pom, String goal ) throws Exception {

        ExecJavaMojo mojo;
        mojo = (ExecJavaMojo) lookupMojo( goal, pom );

        setUpProject( pom, mojo );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );

        // why isn't this set up by the harness based on the default-value?  TODO get to bottom of this!
        setVariableValueToObject( mojo, "includeProjectDependencies", Boolean.TRUE );
        setVariableValueToObject( mojo, "killAfter", new Long( -1 ) );

        assertNotNull( mojo );
        assertNotNull( project );

        // trap System.out
        PrintStream out = System.out;
        StringOutputStream stringOutputStream = new StringOutputStream();
        System.setOut( new PrintStream( stringOutputStream ) );
        // ensure we don't log unnecessary stuff which would interfere with assessing success of tests
        mojo.setLog(new DefaultLog(new ConsoleLogger(Logger.LEVEL_ERROR,"exec:java")));

        try
        {
            mojo.execute();
        }
        catch ( Throwable e )
        {
            e.printStackTrace( System.err );
            fail( e.getMessage() );
        }
        finally
        {
            System.setOut( out );
        }

        return stringOutputStream.toString();
    }

    private void setUpProject( File pomFile, AbstractMojo mojo )
        throws Exception
    {
        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        ArtifactRepositoryLayout localRepositoryLayout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        String path = "src/test/repository";

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local", "file://" +
            new File( path ).getAbsolutePath(), localRepositoryLayout );

        MavenProject project = builder.buildWithDependencies( pomFile, localRepository, null );
        //this gets the classes for these tests of this mojo (exec plugin) onto the project classpath for the test
        project.getBuild().setOutputDirectory( new File("target/test-classes").getAbsolutePath() );
        setVariableValueToObject( mojo, "project", project );
    }
}
