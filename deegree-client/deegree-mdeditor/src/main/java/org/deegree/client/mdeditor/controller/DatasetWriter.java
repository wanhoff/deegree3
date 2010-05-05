//$HeadURL: svn+ssh://lbuesching@svn.wald.intevation.de/deegree/base/trunk/resources/eclipse/files_template.xml $
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
package org.deegree.client.mdeditor.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.deegree.client.mdeditor.config.Configuration;
import org.deegree.client.mdeditor.model.FormGroup;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:buesching@lat-lon.de">Lyn Buesching</a>
 * @author last edited by: $Author: lyn $
 * 
 * @version $Revision: $, $Date: $
 */
public class DatasetWriter extends FormWriter {

    public static void writeElements( List<FormGroup> formGroups ) {
        // TODO
        String title = "title";
        File file = new File( Configuration.getFilesDirURL(), title + ".xml" );

        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            FileOutputStream fos = new FileOutputStream( file );
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter( fos );

            writer.writeStartDocument();
            writer.writeStartElement( DS_ELEM );
            writer.writeAttribute( "id", title );

            for ( FormGroup fg : formGroups ) {
                appendFormGroup( writer, fg );
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        } catch ( FileNotFoundException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( XMLStreamException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
