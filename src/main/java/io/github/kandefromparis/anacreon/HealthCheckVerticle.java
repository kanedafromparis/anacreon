/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.kandefromparis.anacreon;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;

/**
 *
 * @author csabourdin
 */
public class HealthCheckVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckVerticle.class);

    private HttpServer server;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOG.debug("Starting {0} with configuration: {1}", HealthCheckVerticle.class.getSimpleName(), config().encodePrettily());

        startHttpServer().setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
    }

    private Future<HttpServer> startHttpServer() {
        Future<HttpServer> webServerFuture = Future.future();
        Router router = configureRouter();

        int port = config().getInteger("port", 8080);

        LOG.info("Starting HealthCheck web server on port {0,number,#}", port);
        server = vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, webServerFuture.completer());

        return webServerFuture;
    }

    private Router configureRouter() {

        Router router = Router.router(vertx);

        return mapHandler(router);
    }

    public Router mapHandler(Router router) {
        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
        HealthCheckHandler readinessHandler = HealthCheckHandler.create(vertx);

        healthCheckHandler.register(ConfAPICall.LIVENESS.getURL(), this::healthz);
        readinessHandler.register(ConfAPICall.LIVENESS.getURL(), this::readiness);

        LOG.info("Adding route REST API : {0}", HealthCheckVerticle.class.getSimpleName());

        LOG.debug("Adding GET {0}", ConfAPICall.LIVENESS.getURL());
        router.get(ConfAPICall.LIVENESS.getURL()).handler(healthCheckHandler);
        
        LOG.debug("Adding GET {0}", ConfAPICall.READINESS.getURL());
        router.get(ConfAPICall.READINESS.getURL()).handler(readinessHandler);

        return router;
    }

    private void healthz(Future<Status> future) {
        future.complete(Status.OK());
    }

    private void readiness(Future<Status> future) {
        future.complete(Status.OK());
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down webserver providing HealthCheck");
        server.close();
    }
}
