//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
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
package org.deegree.tools.crs.georeferencing.application;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.vecmath.Point2d;

import org.deegree.commons.utils.Pair;
import org.deegree.coverage.raster.io.RasterIOOptions;
import org.deegree.cs.CRS;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.primitive.Ring;
import org.deegree.rendering.r3d.model.geometry.GeometryQualityModel;
import org.deegree.rendering.r3d.model.geometry.SimpleAccessGeometry;
import org.deegree.rendering.r3d.opengl.display.OpenGLEventHandler;
import org.deegree.rendering.r3d.opengl.rendering.model.geometry.WorldRenderableObject;
import org.deegree.tools.crs.georeferencing.application.transformation.Helmert3Transform;
import org.deegree.tools.crs.georeferencing.application.transformation.Polynomial;
import org.deegree.tools.crs.georeferencing.application.transformation.TransformationMethod;
import org.deegree.tools.crs.georeferencing.application.transformation.TransformationMethod.TransformationType;
import org.deegree.tools.crs.georeferencing.communication.BuildingFootprintPanel;
import org.deegree.tools.crs.georeferencing.communication.GRViewerGUI;
import org.deegree.tools.crs.georeferencing.communication.NavigationBarPanel;
import org.deegree.tools.crs.georeferencing.communication.PointTableFrame;
import org.deegree.tools.crs.georeferencing.communication.Scene2DPanel;
import org.deegree.tools.crs.georeferencing.model.Footprint;
import org.deegree.tools.crs.georeferencing.model.MouseModel;
import org.deegree.tools.crs.georeferencing.model.Scene2D;
import org.deegree.tools.crs.georeferencing.model.TextFieldModel;
import org.deegree.tools.crs.georeferencing.model.points.AbstractGRPoint;
import org.deegree.tools.crs.georeferencing.model.points.FootprintPoint;
import org.deegree.tools.crs.georeferencing.model.points.GeoReferencedPoint;
import org.deegree.tools.crs.georeferencing.model.points.Point4Values;
import org.deegree.tools.rendering.viewer.File3dImporter;

/**
 * The <Code>Controller</Code> is responsible to bind the view with the model.
 * 
 * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class Controller {

    private GRViewerGUI view;

    private Scene2D model;

    private Scene2DPanel panel;

    private Scene2DValues sceneValues;

    private BuildingFootprintPanel footPanel;

    private NavigationBarPanel navPanel;

    private PointTableFrame tablePanel;

    private RasterIOOptions options;

    private Footprint footPrint;

    private TextFieldModel textFieldModel;

    private OpenGLEventHandler glHandler;

    private MouseModel mouseGeoRef;

    private MouseModel mouseFootprint;

    private Point2d changePoint;

    private boolean isHorizontalRef, start;

    private GeometryFactory geom;

    private CRS sourceCRS;

    private CRS targetCRS;

    private List<Pair<Point4Values, Point4Values>> mappedPoints;

    private TransformationType transformationType;

    private ParameterStore store;

    public int order;

    public Controller( GRViewerGUI view, Scene2D model, ParameterStore store ) {

        geom = new GeometryFactory();
        options = new RasterOptions( store ).getOptions();
        sceneValues = new Scene2DValues( options, geom );
        this.view = view;
        this.model = model;
        this.panel = view.getScenePanel2D();
        this.footPanel = view.getFootprintPanel();
        this.navPanel = view.getNavigationPanel();
        this.footPrint = new Footprint( sceneValues, geom );
        this.tablePanel = view.getPointTablePanel();
        this.start = false;
        this.glHandler = view.getOpenGLEventListener();
        this.store = store;

        this.mappedPoints = new ArrayList<Pair<Point4Values, Point4Values>>();
        this.footPanel.setOffset( 10 );

        model.init( options, sceneValues );
        view.addMenuItemListener( new ButtonListener() );
        view.addHoleWindowListener( new HoleWindowListener() );
        navPanel.addHorizontalRefListener( new ButtonListener() );

        tablePanel.addHorizontalRefListener( new ButtonListener() );
        tablePanel.addTableModelListener( new TableListener() );

        // init the scenePanel and the mouseinteraction of it
        initGeoReferencingScene();

        // init the footPanel and the mouseinteraction of it
        initFootprintScene();

        isHorizontalRef = false;

    }

    private void initFootprintScene() {
        footPanel.addScene2DMouseListener( new Scene2DMouseListener() );
        footPanel.addScene2DMouseMotionListener( new Scene2DMouseMotionListener() );
        footPanel.addScene2DMouseWheelListener( new Scene2DMouseWheelListener() );

        sceneValues.setDimenstionFootpanel( footPanel.getBounds() );
        mouseFootprint = new MouseModel();
        // TODO at the moment the file which is used is static in the GRViewerGUI!!!
        List<WorldRenderableObject> rese = File3dImporter.open( view, store.getFilename() );
        sourceCRS = null;
        for ( WorldRenderableObject res : rese ) {

            sourceCRS = res.getBbox().getCoordinateSystem();

            glHandler.addDataObjectToScene( res );
        }
        List<float[]> geometryThatIsTaken = new ArrayList<float[]>();
        for ( GeometryQualityModel g : File3dImporter.gm ) {

            ArrayList<SimpleAccessGeometry> h = g.getQualityModelParts();
            boolean isfirstOccurrence = false;
            float minimalZ = 0;

            for ( SimpleAccessGeometry b : h ) {

                float[] a = b.getHorizontalGeometries( b.getGeometry() );
                if ( a != null ) {
                    if ( isfirstOccurrence == false ) {
                        minimalZ = a[2];
                        geometryThatIsTaken.add( a );
                        isfirstOccurrence = true;
                    } else {
                        if ( minimalZ < a[2] ) {

                        } else {
                            geometryThatIsTaken.remove( geometryThatIsTaken.size() - 1 );
                            minimalZ = a[2];
                            geometryThatIsTaken.add( a );
                        }
                    }
                    // System.out.println( a );
                }
            }

        }

        footPrint.generateFootprints( geometryThatIsTaken );

        footPanel.setPolygonList( footPrint.getWorldCoordinateRingList(), sceneValues );

        footPanel.repaint();

    }

    private void initGeoReferencingScene() {
        mouseGeoRef = new MouseModel();
        init();
        targetCRS = sceneValues.getCrs();
        panel.addScene2DMouseListener( new Scene2DMouseListener() );
        panel.addScene2DMouseMotionListener( new Scene2DMouseMotionListener() );
        panel.addScene2DMouseWheelListener( new Scene2DMouseWheelListener() );

    }

    /**
     * 
     * Controls the ActionListener
     * 
     * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
     * @author last edited by: $Author$
     * 
     * @version $Revision$, $Date$
     */
    class ButtonListener implements ActionListener {

        /*
         * (non-Javadoc)
         * 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        @Override
        public void actionPerformed( ActionEvent e ) {
            Object source = e.getSource();
            if ( source instanceof JCheckBox ) {
                if ( ( (JCheckBox) source ).getText().startsWith( NavigationBarPanel.HORIZONTAL_REFERENCING ) ) {
                    if ( isHorizontalRef == false ) {
                        isHorizontalRef = true;
                    } else {
                        isHorizontalRef = false;
                    }
                }

            }
            if ( source instanceof JTextField ) {
                JTextField tF = (JTextField) source;
                if ( tF.getName().startsWith( GRViewerGUI.JTEXTFIELD_COORDINATE_JUMPER ) ) {
                    System.out.println( "put something in the textfield: " + tF.getText() );

                    textFieldModel = new TextFieldModel( tF.getText() );
                    if ( sceneValues.getTransformedBounds() != null ) {
                        AbstractGRPoint translatedPoint = sceneValues.setCentroidRasterEnvelopePosition(
                                                                                                         textFieldModel.getxCoordinate(),
                                                                                                         textFieldModel.getyCoordiante() );

                        System.out.println( textFieldModel.toString() );
                        // if ( textFieldModel.getSpan() != -1 ) {
                        // int[] spanInPixel = sceneValues.getPixelCoord( new GeoReferencedPoint(
                        // textFieldModel.getSpan(),
                        // textFieldModel.getSpan() ) );
                        // panel.setImageToDraw( model.generateSubImage( new Rectangle( spanInPixel[0], spanInPixel[1] )
                        // ) );
                        // } else {
                        sceneValues.moveEnvelope( translatedPoint );
                        panel.setImageToDraw( model.generateSubImageFromRaster( sceneValues.getSubRaster() ) );
                        // }
                        panel.updatePoints( sceneValues );
                        panel.repaint();
                    }
                }

            }
            if ( source instanceof JButton ) {
                if ( ( (JButton) source ).getText().startsWith( PointTableFrame.BUTTON_DELETE_SELECTED ) ) {
                    System.out.println( "you clicked on delete selected" );
                    int[] tableRows = tablePanel.getTable().getSelectedRows();

                    for ( int tableRow : tableRows ) {
                        FootprintPoint pointFromTable = new FootprintPoint(
                                                                            (Double) tablePanel.getModel().getValueAt(
                                                                                                                       tableRow,
                                                                                                                       2 ),
                                                                            (Double) tablePanel.getModel().getValueAt(
                                                                                                                       tableRow,

                                                                                                                       3 ) );
                        boolean contained = false;
                        for ( Pair<Point4Values, Point4Values> p : mappedPoints ) {
                            double x = p.first.getWorldCoords().getX();
                            double y = p.first.getWorldCoords().getY();
                            if ( pointFromTable.getX() == x && pointFromTable.getY() == y ) {
                                removeFromMappedPoints( p );
                                contained = true;
                                panel.removeFromSelectedPoints( p.second );
                                footPanel.removeFromSelectedPoints( p.first );
                                break;
                            }
                        }
                        if ( contained == false ) {
                            footPanel.setLastAbstractPoint( null, null );
                            panel.setLastAbstractPoint( null, null );
                        }

                        tablePanel.removeRow( tableRow );

                    }

                    panel.repaint();
                    footPanel.repaint();
                }
                if ( ( (JButton) source ).getText().startsWith( PointTableFrame.BUTTON_DELETE_ALL ) ) {
                    System.out.println( "you clicked on delete all" );
                    removeAllFromMappedPoints();

                }
                if ( ( (JButton) source ).getText().startsWith( NavigationBarPanel.COMPUTE_BUTTON_NAME ) ) {
                    System.out.println( "you clicked on computation" );

                    // swap the tempPoints into the map now
                    if ( footPanel.getLastAbstractPoint() != null && panel.getLastAbstractPoint() != null ) {
                        setValues();
                    }
                    System.out.println( sourceCRS + " " + targetCRS );
                    TransformationMethod transform = null;
                    if ( transformationType == null ) {
                        transformationType = TransformationType.PolynomialFirstOrder;
                        order = 1;
                    }
                    switch ( transformationType ) {

                    case PolynomialFirstOrder:
                        // transform = new PolynomialFirstOrder( mappedPoints, footPrint, sceneValues, sourceCRS,
                        // targetCRS, 1 );
                        // transform = new LeastSquarePolynomial( mappedPoints, footPrint, sceneValues, sourceCRS,
                        // targetCRS, order );

                        transform = new Polynomial( mappedPoints, footPrint, sceneValues, sourceCRS, targetCRS, order );
                        break;
                    case Helmert_3:
                        transform = new Helmert3Transform( mappedPoints, footPrint, sceneValues, sourceCRS, targetCRS,
                                                           order );
                        // transform = new Polynomial( mappedPoints, footPrint, sceneValues, sourceCRS, targetCRS, order
                        // );
                        break;
                    }
                    List<Ring> polygonRing = transform.computeRingList();

                    panel.setPolygonList( polygonRing, sceneValues );

                    panel.repaint();
                }
            }
            if ( source instanceof JMenuItem ) {

                if ( ( (JMenuItem) source ).getText().startsWith( GRViewerGUI.MENUITEM_TRANS_POLYNOM_FIRST ) ) {
                    transformationType = TransformationMethod.TransformationType.PolynomialFirstOrder;
                    order = 1;

                }
                if ( ( (JMenuItem) source ).getText().startsWith( GRViewerGUI.MENUITEM_TRANS_POLYNOM_SECOND ) ) {
                    transformationType = TransformationMethod.TransformationType.PolynomialFirstOrder;
                    order = 2;

                }
                if ( ( (JMenuItem) source ).getText().startsWith( GRViewerGUI.MENUITEM_TRANS_POLYNOM_THIRD ) ) {
                    transformationType = TransformationMethod.TransformationType.PolynomialFirstOrder;
                    order = 3;

                }
                if ( ( (JMenuItem) source ).getText().startsWith( GRViewerGUI.MENUITEM_TRANS_HELMERT ) ) {
                    transformationType = TransformationMethod.TransformationType.Helmert_3;
                    order = 1;
                }

            }

        }
    }

    class TableListener implements TableModelListener {

        @Override
        public void tableChanged( TableModelEvent e ) {

        }

    }

    /**
     * 
     * Controls the MouseListener
     * 
     * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
     * @author last edited by: $Author$
     * 
     * @version $Revision$, $Date$
     */
    class Scene2DMouseListener implements MouseListener {

        @Override
        public void mouseClicked( MouseEvent arg0 ) {

        }

        @Override
        public void mouseEntered( MouseEvent arg0 ) {

        }

        @Override
        public void mouseExited( MouseEvent arg0 ) {

        }

        @Override
        public void mousePressed( MouseEvent m ) {
            Object source = m.getSource();
            if ( source instanceof JPanel ) {
                // Scene2DPanel
                if ( ( (JPanel) source ).getName().equals( Scene2DPanel.SCENE2D_PANEL_NAME ) ) {
                    mouseGeoRef.setPointMousePressed( new Point2d( m.getX(), m.getY() ) );
                }
                if ( ( (JPanel) source ).getName().equals( BuildingFootprintPanel.BUILDINGFOOTPRINT_PANEL_NAME ) ) {
                    mouseFootprint.setPointMousePressed( new Point2d( m.getX(), m.getY() ) );
                }
            }

        }

        @Override
        public void mouseReleased( MouseEvent m ) {
            Object source = m.getSource();
            if ( source instanceof JPanel ) {
                // Scene2DPanel
                if ( ( (JPanel) source ).getName().equals( Scene2DPanel.SCENE2D_PANEL_NAME ) ) {

                    if ( isHorizontalRef == true ) {
                        if ( start == false ) {
                            start = true;
                            footPanel.setFocus( false );
                            panel.setFocus( true );
                        }
                        if ( footPanel.getLastAbstractPoint() != null && panel.getLastAbstractPoint() != null
                             && panel.getFocus() == true ) {
                            setValues();
                        }
                        if ( footPanel.getLastAbstractPoint() == null && panel.getLastAbstractPoint() == null
                             && panel.getFocus() == true ) {
                            tablePanel.addRow();
                        }
                        double x = m.getX();
                        double y = m.getY();
                        GeoReferencedPoint geoReferencedPoint = new GeoReferencedPoint( x, y );
                        GeoReferencedPoint g = (GeoReferencedPoint) sceneValues.getWorldPoint( geoReferencedPoint );
                        panel.setLastAbstractPoint( geoReferencedPoint, g );
                        tablePanel.setCoords( panel.getLastAbstractPoint().getWorldCoords() );

                    } else {
                        mouseGeoRef.setMouseChanging( new GeoReferencedPoint(
                                                                              ( mouseGeoRef.getPointMousePressed().getX() - m.getX() ),
                                                                              ( mouseGeoRef.getPointMousePressed().getY() - m.getY() ) ) );

                        sceneValues.moveEnvelope( mouseGeoRef.getMouseChanging() );
                        panel.setImageToDraw( model.generateSubImageFromRaster( sceneValues.getSubRaster() ) );
                        panel.updatePoints( sceneValues );
                    }

                    panel.repaint();
                }
                // footprintPanel
                if ( ( (JPanel) source ).getName().equals( BuildingFootprintPanel.BUILDINGFOOTPRINT_PANEL_NAME ) ) {
                    if ( isHorizontalRef == true ) {

                        if ( start == false ) {
                            start = true;
                            footPanel.setFocus( true );
                            panel.setFocus( false );
                        }
                        if ( footPanel.getLastAbstractPoint() != null && panel.getLastAbstractPoint() != null
                             && footPanel.getFocus() == true ) {
                            setValues();
                        }
                        if ( footPanel.getLastAbstractPoint() == null && panel.getLastAbstractPoint() == null
                             && footPanel.getFocus() == true ) {
                            tablePanel.addRow();
                        }
                        double x = m.getX();
                        double y = m.getY();
                        Pair<AbstractGRPoint, FootprintPoint> point = footPanel.getClosestPoint( new FootprintPoint( x,
                                                                                                                     y ) );
                        footPanel.setLastAbstractPoint( point.first, point.second );
                        tablePanel.setCoords( footPanel.getLastAbstractPoint().getWorldCoords() );

                    } else {
                        mouseFootprint.setMouseChanging( new FootprintPoint(
                                                                             ( mouseFootprint.getPointMousePressed().getX() - m.getX() ),
                                                                             ( mouseFootprint.getPointMousePressed().getY() - m.getY() ) ) );

                        sceneValues.moveEnvelope( mouseFootprint.getMouseChanging() );
                        footPanel.updatePoints( sceneValues );
                    }
                    footPanel.repaint();
                }
            }
        }

    }

    /**
     * Sets values to the JTableModel and adds a new row to it.
     */
    private void setValues() {

        footPanel.addToSelectedPoints( footPanel.getLastAbstractPoint() );
        panel.addToSelectedPoints( panel.getLastAbstractPoint() );
        addToMappedPoints( footPanel.getLastAbstractPoint(), panel.getLastAbstractPoint() );
        footPanel.setLastAbstractPoint( null, null );
        panel.setLastAbstractPoint( null, null );

    }

    /**
     * 
     * The <Code>Prediction</Code> class should handle the getImage in the background.
     * 
     * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
     * @author last edited by: $Author$
     * 
     * @version $Revision$, $Date$
     */
    class Prediction extends Thread {

        Point2d changing;

        public Prediction( Point2d changing ) {
            this.changing = changing;

        }

        public void run() {

            changePoint = new Point2d( changing.getX(), changing.getY() );
            System.out.println( "Threadchange: " + changePoint );

            // model.generatePredictedImage( changing );
            // model.generateImage( changing );

        }

    }

    /**
     * 
     * TODO add class documentation here
     * 
     * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
     * @author last edited by: $Author$
     * 
     * @version $Revision$, $Date$
     */
    class Scene2DMouseMotionListener implements MouseMotionListener {

        @Override
        public void mouseDragged( MouseEvent arg0 ) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseMoved( MouseEvent m ) {

            Object source = m.getSource();

            if ( source instanceof JPanel ) {
                // Scene2DPanel
                if ( ( (JPanel) source ).getName().equals( Scene2DPanel.SCENE2D_PANEL_NAME ) ) {
                    // System.out.println( m.getPoint() );
                    if ( mouseGeoRef != null ) {
                        mouseGeoRef.setMouseMoved( new GeoReferencedPoint( m.getX(), m.getY() ) );
                    }
                }
                // footprintPanel
                if ( ( (JPanel) source ).getName().equals( BuildingFootprintPanel.BUILDINGFOOTPRINT_PANEL_NAME ) ) {
                    // System.out.println( m.getPoint() );
                    if ( mouseFootprint != null ) {
                        mouseFootprint.setMouseMoved( new FootprintPoint( m.getX(), m.getY() ) );
                    }
                }
            }
        }

    }

    /**
     * 
     * Represents the zoom function.
     * 
     * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
     * @author last edited by: $Author$
     * 
     * @version $Revision$, $Date$
     */
    class Scene2DMouseWheelListener implements MouseWheelListener {
        private boolean zoomIn = false;

        private float resizing;

        private AbstractGRPoint mouseOver;

        @Override
        public void mouseWheelMoved( MouseWheelEvent m ) {

            Object source = m.getSource();

            if ( source instanceof JPanel ) {
                // Scene2DPanel
                if ( ( (JPanel) source ).getName().equals( Scene2DPanel.SCENE2D_PANEL_NAME ) ) {
                    mouseOver = mouseGeoRef.getMouseMoved();
                    resizing = .05f;
                    if ( m.getWheelRotation() < 0 ) {
                        zoomIn = true;
                    } else {
                        zoomIn = false;
                    }
                    sceneValues.computeZoomedEnvelope( zoomIn, resizing, mouseOver );
                    panel.setImageToDraw( model.generateSubImageFromRaster( sceneValues.getSubRaster() ) );
                    panel.updatePoints( sceneValues );
                    panel.repaint();
                }
                // footprintPanel
                if ( ( (JPanel) source ).getName().equals( BuildingFootprintPanel.BUILDINGFOOTPRINT_PANEL_NAME ) ) {

                    resizing = .1f;
                    mouseOver = mouseFootprint.getMouseMoved();
                    if ( m.getWheelRotation() < 0 ) {
                        zoomIn = true;
                    } else {
                        zoomIn = false;
                    }
                    sceneValues.computeZoomedEnvelope( zoomIn, resizing, mouseOver );
                    footPanel.updatePoints( sceneValues );
                    footPanel.repaint();
                }
            }

        }

    }

    /**
     * 
     * Controls the ComponentListener
     * 
     * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
     * @author last edited by: $Author$
     * 
     * @version $Revision$, $Date$
     */
    class HoleWindowListener implements ComponentListener {

        @Override
        public void componentHidden( ComponentEvent arg0 ) {
            // TODO Auto-generated method stub

        }

        @Override
        public void componentMoved( ComponentEvent arg0 ) {
            // TODO Auto-generated method stub

        }

        @Override
        public void componentResized( ComponentEvent c ) {
            if ( model.getGeneratedImage() != null ) {
                init();

            }

        }

        @Override
        public void componentShown( ComponentEvent arg0 ) {
            // TODO Auto-generated method stub

        }

    }

    /**
     * Initializes the computing and the painting of the georeferenced map.
     */
    private void init() {

        sceneValues.setImageDimension( new Rectangle( panel.getBounds().width, panel.getBounds().height ) );
        panel.setImageDimension( sceneValues.getImageDimension() );

        panel.setImageToDraw( model.generateSubImage( sceneValues.getImageDimension() ) );

        panel.repaint();
    }

    /**
     * Adds the <Code>AbstractPoint</Code>s to a map
     * 
     * @param mappedPointKey
     * @param mappedPointValue
     */
    private void addToMappedPoints( Point4Values mappedPointKey, Point4Values mappedPointValue ) {
        if ( mappedPointKey != null && mappedPointValue != null ) {
            this.mappedPoints.add( new Pair<Point4Values, Point4Values>( mappedPointKey, mappedPointValue ) );
        }

    }

    private void updateMappedPoints() {
        for ( Pair<Point4Values, Point4Values> pair : mappedPoints ) {
            List<Point4Values> points = footPanel.getSelectedPoints();
            for ( Point4Values p : points ) {
                if ( p.getOldValue() == pair.first.getOldValue() ) {
                    pair.first = new Point4Values( p.getOldValue(), p.getInitialValue(), p.getNewValue(),
                                                   p.getWorldCoords() );
                    break;
                }
            }

        }
    }

    private void removeFromMappedPoints( Pair<Point4Values, Point4Values> pointFromTable ) {
        if ( pointFromTable != null ) {
            mappedPoints.remove( pointFromTable );
        }
    }

    private void removeAllFromMappedPoints() {
        mappedPoints = new ArrayList<Pair<Point4Values, Point4Values>>();
        tablePanel.removeAllRows();
        panel.removeAllFromSelectedPoints();
        footPanel.removeAllFromSelectedPoints();
        footPanel.setLastAbstractPoint( null, null );
        panel.setPolygonList( null, null );
        panel.setLastAbstractPoint( null, null );
        panel.repaint();
        footPanel.repaint();
        panel.setFocus( false );
        footPanel.setFocus( false );
        start = false;

    }

}
