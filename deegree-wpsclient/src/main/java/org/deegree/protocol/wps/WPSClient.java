//$HeadURL: svn+ssh://georg@svn.wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.protocol.wps;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.deegree.commons.xml.XMLAdapter;
import org.deegree.protocol.wps.execute.ExecuteResponse;
import org.deegree.protocol.wps.getcapabilities.WPSCapabilities;
import org.deegree.protocol.wps.tools.InputObject;
import org.deegree.protocol.wps.tools.LoadFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:name@deegree.org">Your Name</a>
 * @author last edited by: $Author: Admin $
 * 
 * @version $Revision: $, $Date: $
 */
public class WPSClient {

    private static Logger LOG = LoggerFactory.getLogger( WPSClient.class );

    private final WPSCapabilities serviceCapabilities;

    private String executeURL;

    private String describeProcessURL;

    private List<InputObject> inputObjectList = new ArrayList();

    /**
     * Public constructor to access a WPS instance based on it's GetCapabilities URL
     * 
     * @param capabilitiesURL
     *            url to a WPS instance
     * @throws MalformedURLException
     *             in case a GetCapabilities URL could not constructed from WPS Capabilities response
     */
    public WPSClient( URL capabilitiesURL ) throws MalformedURLException {
        try {
            serviceCapabilities = new WPSCapabilities( new XMLAdapter( capabilitiesURL ) );
            describeProcessURL = serviceCapabilities.getOperationURLasString( "DescribeProcess", true );
            if ( !describeProcessURL.endsWith( "?" ) ) {
                describeProcessURL += "?";
            }
            LOG.debug( "Using '" + describeProcessURL + "' for DescribeProcess requests (GET)." );
            executeURL = serviceCapabilities.getOperationURLasString( "Execute", false );
            if ( executeURL.endsWith( "?" ) ) {
                executeURL = executeURL.substring( 0, executeURL.length() - 1 );
            }
            LOG.debug( "Using '" + executeURL + "' for Execute requests (POST)." );
        } catch ( Exception e ) {
            LOG.error( e.getLocalizedMessage(), e );
            throw new NullPointerException( "Could not read from URL: " + capabilitiesURL + " error was: "
                                            + e.getLocalizedMessage() );
        }
    }

    /**
     * 
     * @return String[] identifiers of all processes deliverd by the WPS
     */
    public String[] getProcessIdentifiers() {
        int size = this.serviceCapabilities.getProcessOfferings().size();
        String[] identifier = new String[size];

        for ( int i = 0; i < size; i++ ) {
            identifier[i] = serviceCapabilities.getProcessOfferings().get( i ).getIdentifier();
        }

        return identifier;

    }

    /**
     * 
     * @return ProcessInfo Object which holds all Information of the DescribeProcess Document
     * 
     * @param processIdentifier
     *            identifier of the process
     */
    public ProcessInfo getProcessInfo( String processIdentifier ) {
        return ( new ProcessInfo( describeProcessURL, processIdentifier ) );
    }

    /**
     * 
     * @return String response of the executeRequest as a String
     * 
     * @param InputObject
     *            [] Input of the process
     * 
     * @param processIdentifier
     *            identifier of the process
     * 
     */
    public String executeProcessStringResult( InputObject[] inputobject, String processIdentifier ) {

        ProcessExecution processExecution = new ProcessExecution(
                                                                  getProcessInfo( processIdentifier ).getProcessDescription(),
                                                                  executeURL );

        for ( int i = 0; i < inputObjectList.size(); i++ ) {
            processExecution.addInput( this.inputObjectList.get( i ) );
        }
        inputObjectList = null;

        return processExecution.sendExecuteRequestStringReturn();
    }

    /**
     * 
     * @return Object result the of process as object
     * 
     * @param InputObject
     *            [] Input of the process
     * 
     * @param processIdentifier
     *            identifier of the process
     * 
     */
    public Object executeProcessObejctResult( InputObject[] inputobject, String processIdentifier ) {

        ProcessExecution processExecution = new ProcessExecution(
                                                                  getProcessInfo( processIdentifier ).getProcessDescription(),
                                                                  executeURL );

        for ( int i = 0; i < inputobject.length; i++ ) {
            processExecution.addInput( inputobject[i] );
        }

        return processExecution.sendExecuteRequestExecuteObjectReturn();
    }

    /**
     * 
     * @return Object result the of process as ExecuteResponse object
     * 
     * @param InputObject
     *            [] Input of the process
     * 
     * @param processIdentifier
     *            identifier of the process
     * 
     */
    public ExecuteResponse executeProcessExecuteResponseResult( InputObject[] inputobject, String processIdentifier ) {

        ProcessExecution processExecution = new ProcessExecution(
                                                                  getProcessInfo( processIdentifier ).getProcessDescription(),
                                                                  describeProcessURL );

        for ( int i = 0; i < this.inputObjectList.size(); i++ ) {
            processExecution.addInput( this.inputObjectList.get( i ) );
        }
        inputObjectList = null;

        return processExecution.sendExecuteRequestExecuteResponseReturn();
    }

    /**
     * 
     * @return InputObject input of the process
     * 
     * @param identifier
     *            identifier of the input
     * 
     * @param input
     * 
     */
    public InputObject setInputasObject( String identifier, Object input ) {

        InputObject inputObject = new InputObject( identifier, input, false );

        return inputObject;
    }

    /**
     * 
     * @return InputObject input of the process
     * 
     * @param identifier
     *            identifier of the input
     * 
     * @param filePath
     *            path to the file of the input
     * 
     */
    public InputObject setInputasFile( String identifier, String filePath )
                            throws Exception {
        LoadFile loadFile = new LoadFile( filePath );
        Object object = loadFile.load();

        InputObject inputObject = new InputObject( identifier, object, false );
        return inputObject;
    }

    /**
     * 
     * @return InputObject input of the process
     * 
     * @param identifier
     *            identifier of the input
     * 
     * @param url
     *            of the input
     * 
     */
    public InputObject setInputasURL( String identifier, String url ) {

        InputObject inputObject = new InputObject( identifier, url, true );
        return inputObject;
    }

    public void setOutput() {

    }
}
