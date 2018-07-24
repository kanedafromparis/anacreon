/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.kandefromparis.anacreon;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author csabourdin
 */
@RunWith(VertxUnitRunner.class)
public class StartVerticleTest {
    

    private Vertx vertx;
    private Integer port = 8080;
    private String host = "127.0.0.1";

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        // Let's configure the verticle to listen on the 'test' port (randomly picked).
        // We create deployment options and set the _configuration_ json object:

        vertx.deployVerticle(StartVerticle.class.getName(), context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }


    /**
     * Test of start method, of class StartVerticle.
     */
    @Test
    public void testStart() {
        System.out.println("start");
////        Future<Void> fut = null;
////        StartVerticle instance = new StartVerticle();
////        instance.start(vertx);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
    
}
