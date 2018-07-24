/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.kandefromparis.anacreon;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;


/**
 *
 * @author csabourdin
 */
public class StartVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(StartVerticle.class);

    @Override
    public void start(Future<Void> fut) {
        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);

        LOG.debug("Starting {0} with configuration: {1}", StartVerticle.class.getSimpleName(), config().encodePrettily());

        
        // Create a router object.
        Router router = Router.router(vertx);

        	router.route().handler(BodyHandler.create());//.setUploadsDirectory("files"));
	router.route().handler(CookieHandler.create());
	router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
	
        //@todo look into 
        // https://www.programcreek.com/java-api-examples/?code=JoMingyu/Server-Quickstart-Vert.x/Server-Quickstart-Vert.x-master/Vert.x-Server-Quickstart/src/main/java/com/planb/main/MainVerticle.java#
        // for CORSHandler and LogHandler
        //router.route().handler(CORSHandler.create());
	//router.route().handler(LogHandler.create());
        // Serve static resources from the /assets directory
        router.route("/assets/*").handler(StaticHandler.create("assets"));
        router.get("/").handler(this::getRedirect);

        HealthCheckVerticle healthcheck = new HealthCheckVerticle();
        healthcheck.init(vertx, context);
        healthcheck.mapHandler(router);

        BuildCallVerticle buildCall = new BuildCallVerticle();
        buildCall.init(vertx, context);
        buildCall.mapHandler(router);

        vertx
                .createHttpServer(options)
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }

    private void getRedirect(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(307)
                .putHeader("Location", "/assets/index.html")
                .end();
    }

}
