<filename>src/se/treplex/sketchytruck/PhysicsEditorShapeLibrary.java<fim_prefix>

package se.treplex.sketchytruck;

/**
 * PhysicsEditor Importer Library
 *
 * Usage:
 * - Create an instance of this class
 * - Use the "open" method to load an XML file from PhysicsEditor
 * - Invoke "createBody" to create bodies from library.
 *
 * by Adrian Nilsson (ade at ade dot se)
 * BIG IRON GAMES (bigirongames.org)
 * Date: 2011-08-30
 * Time: 11:51
 */

import android.content.Context;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import org.anddev.andengine.entity.shape.IShape;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.InternalStorageFileBitmapTextureAtlasSource;
import org.anddev.andengine.util.constants.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.HashMap;

public class PhysicsEditorShapeLibrary {
    private HashMap<String, BodyTemplate> shapes = new HashMap<String, BodyTemplate>();
    private float pixelToMeterRatio = PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

    public PhysicsEditorShapeLibrary() {
        //Default constructor
    }
    public PhysicsEditorShapeLibrary(float pixelToMeterRatio) {
        this.pixelToMeterRatio = pixelToMeterRatio;
    }

    /**
     * Read shapes from an XML file and add to library.
     * Path is relative to your assets folder so if your file is in
     * "assets/shapes/shapes.xml", the path should be "shapes/shapes.xml"
     * @param context
     * @param xmlFile
     */
    public void open(Context context, String xmlFile) {
        append(context, xmlFile, this.pixelToMeterRatio);
    }

    /**
     * If you wish, you may access the template data and create custom bodies.
     * Be advised that vertex positions are pre-adjusted for Box2D coordinates (pixel to meter ratio).
     * @param key
     * @return
     */
    public BodyTemplate get(String key) {
        return this.shapes.get(key);
    }

    /**
     * Create a Box2D Body with a shape from the library.
     * @param name name of the shape (usually filename of the image, without extension)
     * @param pShape the AndEngine shape you will be associating the body with (physics connector is not created here).
     * @param pPhysicsWorld the AndEngine Box2D physics world object.
     * @return
     */
    public Body createBody(String name, IShape pShape, PhysicsWorld pPhysicsWorld) {
        BodyTemplate bodyTemplate = this.shapes.get(name);

        final BodyDef boxBodyDef = new BodyDef();
        boxBodyDef.type = bodyTemplate.isDynamic ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody;

        final float[] sceneCenterCoordinates = pShape.getSceneCenterCoordinates();
        boxBodyDef.position.x = sceneCenterCoordinates[Constants.VERTEX_INDEX_X] / this.pixelToMeterRatio;
        boxBodyDef.position.y = sceneCenterCoordinates[Constants.VERTEX_INDEX_Y] / this.pixelToMeterRatio;

        final Body boxBody = pPhysicsWorld.createBody(boxBodyDef);

        for(FixtureTemplate fixtureTemplate : bodyTemplate.fixtureTemplates) {
            for(int i = 0; i < fixtureTemplate.polygons.length; i++) {
                final PolygonShape polygonShape = new PolygonShape();
                final FixtureDef fixtureDef = fixtureTemplate.fixtureDef;

                polygonShape.set(fixtureTemplate.polygons[i].vertices);

                fixtureDef.shape = polygonShape;
                boxBody.createFixture(fixtureDef);
                polygonShape.dispose();
            }
        }

        return boxBody;
    }

    private void append(Context context, String name, float pixelToMeterRatio) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        <fim_suffix>
        try {
            SAXParser parser = factory.newSAXParser();
            ShapeLoader handler = new ShapeLoader(shapes, pixelToMeterRatio);
            parser.parse(context.getAssets().open(name), handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static class ShapeLoader extends DefaultHandler {
        public static final String TAG_BODY = "body";
        public static final String TAG_FIXTURE = "fixture";
        public static final String TAG_POLYGON = "polygon";
        public static final String TAG_VERTEX = "vertex";
        public static final String TAG_NAME = "name";
        public static final String TAG_X = "x";
        public static final String TAG_Y = "y";
        public static final String TAG_DENSITY = "density";
        public static final String TAG_RESTITUTION = "restitution";
        public static final String TAG_FRICTION = "friction";
        public static final String TAG_FILTER_CATEGORY_BITS = "filter_categoryBits";
        public static final String TAG_FILTER_GROUP_INDEX = "filter_groupIndex";
        public static final String TAG_FILTER_MASK_BITS = "filter_maskBits";
        public static final String TAG_ISDYNAMIC = "dynamic";
        public static final String TAG_ISSENSOR = "isSensor";

        private float pixelToMeterRatio;
        private StringBuilder builder;
        private HashMap<String, BodyTemplate> shapes;
        private BodyTemplate currentBody;
        private ArrayList<Vector2> currentPolygonVertices = new ArrayList<Vector2>();
        private ArrayList<FixtureTemplate> currentFixtures = new ArrayList<FixtureTemplate>();
        private ArrayList<PolygonTemplate> currentPolygons = new ArrayList<PolygonTemplate>();



        protected ShapeLoader(HashMap<String, BodyTemplate> shapes, float pixelToMeterRatio) {
            this.shapes = shapes;
            this.pixelToMeterRatio = pixelToMeterRatio;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            builder.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            super.endElement(uri, localName, name);

            if (localName.equalsIgnoreCase(TAG_POLYGON)) {
                currentPolygons.add(new PolygonTemplate(currentPolygonVertices));
            } else if (localName.equalsIgnoreCase(TAG_FIXTURE)) {
                currentFixtures.get(currentFixtures.size()-1).setPolygons(currentPolygons);
            } else if (localName.equalsIgnoreCase(TAG_BODY)) {
                currentBody.setFixtures(currentFixtures);
                shapes.put(currentBody.name, currentBody);
            }

            builder.setLength(0);
        }

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            builder = new StringBuilder();
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, name, attributes);
            builder.setLength(0);
            if (localName.equalsIgnoreCase(TAG_BODY)) {
                this.currentFixtures.clear();
                this.currentBody = new BodyTemplate();
                this.currentBody.name = attributes.getValue(TAG_NAME);
                this.currentBody.isDynamic = attributes.getValue(TAG_ISDYNAMIC).equalsIgnoreCase("true");
            } else if (localName.equalsIgnoreCase(TAG_FIXTURE)) {
                FixtureTemplate fixture = new FixtureTemplate();
                currentPolygons.clear();
                float restitution = Float.parseFloat(attributes.getValue(TAG_RESTITUTION));
                float friction = Float.parseFloat(attributes.getValue(TAG_FRICTION));
                float density = Float.parseFloat(attributes.getValue(TAG_DENSITY));
                short category = parseShort(attributes.getValue(TAG_FILTER_CATEGORY_BITS));
                short groupIndex = parseShort(attributes.getValue(TAG_FILTER_GROUP_INDEX));
                short maskBits = parseShort(attributes.getValue(TAG_FILTER_MASK_BITS));
                boolean isSensor = attributes.getValue(TAG_ISSENSOR).equalsIgnoreCase("true");
                fixture.fixtureDef = PhysicsFactory.createFixtureDef(density, restitution, friction, isSensor, category, maskBits, groupIndex);
                currentFixtures.add(fixture);
            } else if (localName.equalsIgnoreCase(TAG_POLYGON)) {
                currentPolygonVertices.clear();
            }  else if (localName.equalsIgnoreCase(TAG_VERTEX)) {
                currentPolygonVertices.add(new Vector2(Float.parseFloat(attributes.getValue(TAG_X)) / this.pixelToMeterRatio, Float.parseFloat(attributes.getValue(TAG_Y)) / this.pixelToMeterRatio));
            }
        }
    }
    private static short parseShort(String val) {
        int intVal = Integer.parseInt(val);
        short ret = (short)(intVal & 65535);
        return (short)intVal;
    }
    private static class BodyTemplate {
        public String name;
        public boolean isDynamic = true;
        public FixtureTemplate[] fixtureTemplates;
        public void setFixtures(ArrayList<FixtureTemplate> fixtureTemplates) {
            this.fixtureTemplates = fixtureTemplates.toArray(new FixtureTemplate[fixtureTemplates.size()]);
        }
    }
    private static class FixtureTemplate {
        public PolygonTemplate[] polygons;
        public FixtureDef fixtureDef;
        public void setPolygons(ArrayList<PolygonTemplate> polygonTemplates) {
            polygons = polygonTemplates.toArray(new PolygonTemplate[polygonTemplates.size()]);
        }
    }
    private static class PolygonTemplate {
        public Vector2[] vertices;
        public PolygonTemplate(ArrayList<Vector2> vectorList) {
            vertices = vectorList.toArray(new Vector2[vectorList.size()]);
        }
    }
}

<fim_middle>