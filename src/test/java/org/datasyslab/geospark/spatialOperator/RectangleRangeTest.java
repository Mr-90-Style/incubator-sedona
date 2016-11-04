package org.datasyslab.geospark.spatialOperator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.datasyslab.geospark.enums.FileDataSplitter;
import org.datasyslab.geospark.enums.IndexType;
import org.datasyslab.geospark.spatialRDD.RectangleRDD;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;


public class RectangleRangeTest {
    public static JavaSparkContext sc;
    static Properties prop;
    static InputStream input;
    static String InputLocation;
    static Integer offset;
    static FileDataSplitter splitter;
    static IndexType indexType;
    static Integer numPartitions;
    static Envelope queryEnvelope;
    static int loopTimes;
    @BeforeClass
    public static void onceExecutedBeforeAll() {
        SparkConf conf = new SparkConf().setAppName("RectangleRange").setMaster("local[2]");
        sc = new JavaSparkContext(conf);
        Logger.getLogger("org").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
        prop = new Properties();
        input = RectangleRangeTest.class.getClassLoader().getResourceAsStream("rectangle.test.properties");

        //Hard code to a file in resource folder. But you can replace it later in the try-catch field in your hdfs system.
        InputLocation = "file://"+RectangleRangeTest.class.getClassLoader().getResource("primaryroads.csv").getPath();

        offset = 0;
        splitter = null;
        indexType = null;
        numPartitions = 0;

        try {
            // load a properties file
            prop.load(input);
            // There is a field in the property file, you can edit your own file location there.
            // InputLocation = prop.getProperty("inputLocation");
            InputLocation = "file://"+RectangleRangeTest.class.getClassLoader().getResource(prop.getProperty("inputLocation")).getPath();
            offset = Integer.parseInt(prop.getProperty("offset"));
            splitter = FileDataSplitter.valueOf(prop.getProperty("splitter").toUpperCase());
            indexType = IndexType.valueOf(prop.getProperty("indexType").toUpperCase());
            numPartitions = Integer.parseInt(prop.getProperty("numPartitions"));
            queryEnvelope=new Envelope (-90.01,-80.01,30.01,40.01);
            loopTimes=5;
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @AfterClass
    public static void TearDown() {
        sc.stop();
    }

    @Test
    public void testSpatialRangeQuery() throws Exception {
    	RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter);
    	for(int i=0;i<loopTimes;i++)
    	{
    		long resultSize = RangeQuery.SpatialRangeQuery(rectangleRDD, queryEnvelope, 0).getRawRectangleRDD().count();
    		assert resultSize>-1;
    	}
    	assert RangeQuery.SpatialRangeQuery(rectangleRDD, queryEnvelope, 0).getRawRectangleRDD().take(10).get(1).getUserData().toString()!=null;
    }
    @Test
    public void testSpatialRangeQueryUsingIndex() throws Exception {
    	RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter);
    	rectangleRDD.buildIndex(IndexType.RTREE);
    	for(int i=0;i<loopTimes;i++)
    	{
    		long resultSize = RangeQuery.SpatialRangeQueryUsingIndex(rectangleRDD, queryEnvelope, 0).getRawRectangleRDD().count();
    		assert resultSize>-1;
    	}
    	assert RangeQuery.SpatialRangeQueryUsingIndex(rectangleRDD, queryEnvelope, 0).getRawRectangleRDD().take(10).get(1).getUserData().toString()!=null;
    }

}