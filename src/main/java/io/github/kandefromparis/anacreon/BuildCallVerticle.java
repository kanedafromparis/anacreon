/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.kandefromparis.anacreon;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.io.File;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author csabourdin
 */
public class BuildCallVerticle extends AbstractVerticle implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildCallVerticle.class);

    public static final String REST_BUILDCALL = "/buildstart";
    public static final String BUILDCALL_JSON_PARAM = "XXX-BC-ID";
    public static final String WEBHOOK = "webhook";
    public static final String NAMESPACE = "namespace";
    public static final String BC_URI = "bc-generic-webhook-uri";

    public static final String NAME = "name";
    public static final String PORT = "port";
    public static final String SSL = "ssl";
    public static final String TRUSTALL = "trustall";
    public static final String PEMCERTPATH = "pemcertpath";

    private HttpServer server;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        //https://docs.oracle.com/javase/10/docs/api/java/text/MessageFormat.html
        LOG.info("Starting {0} with configuration: {1}", BuildCallVerticle.class.getSimpleName(), config().encodePrettily());

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

        LOG.debug("Starting BuildCall web server on port {0,number,#}", port);

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
        LOG.info("Adding route REST API : {0}", BuildCallVerticle.class.getSimpleName());
        //router.get(REST_BUILDCALL).handler(this::handle);
        LOG.debug("Adding GET {0}", ConfAPICall.API_1_0_BUILD.getURL());
        router.get(ConfAPICall.API_1_0_BUILD.getURL()).handler(this::JustOK);

        LOG.debug("Adding POST {0}", ConfAPICall.API_1_0_BUILD.getURL());
        //router.post(REST_BUILDCALL).handler(this::handle);
        router.post(ConfAPICall.API_1_0_BUILD.getURL()).handler(this::handle);

        LOG.debug("Adding get {0}", ConfAPICall.RANDOM_UUID.getURL());
        //router.post(REST_BUILDCALL).handler(this::handle);
        router.get(ConfAPICall.RANDOM_UUID.getURL()).handler(this::getRandomUUID);
        LOG.debug("Adding post {0}", ConfAPICall.RANDOM_UUID.getURL());
        //router.post(REST_BUILDCALL).handler(this::handle);
        router.post(ConfAPICall.RANDOM_UUID.getURL()).handler(this::getRandomUUID);

        return router;
    }

    private void callBuild(Future<Status> future) {
        future.complete(Status.OK());
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Shutting down webserver providing HealthCheck");
        server.close();
    }

    public void JustOK(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "text/html; charset=utf-8")
                .setStatusCode(200)
                .end(" Just OK ");
    }

    public void getRandomUUID(RoutingContext routingContext) {

        JsonObject conf = new JsonObject();
        conf.put(NAMESPACE, StringUtils.EMPTY);
        conf.put(BC_URI, StringUtils.EMPTY);
        LOG.debug("bodylenght : {0} ", routingContext.getBody().length());

        if (routingContext.getBody().length() > 0) {
            JsonObject bodyAsJson = routingContext.getBodyAsJson();
            if ((bodyAsJson != null)
                    && (!bodyAsJson.isEmpty())) {
                if ((bodyAsJson.containsKey(NAMESPACE))
                        && (bodyAsJson.getString(NAMESPACE) != null)) {
                    conf.put(NAMESPACE, routingContext.getBodyAsJson().getString(NAMESPACE));
                }
                if ((bodyAsJson.containsKey(BC_URI))
                        && (bodyAsJson.getString(BC_URI) != null)) {
                    conf.put(BC_URI, routingContext.getBodyAsJson().getString(BC_URI));
                }
            }
        }
        if (config() == null) {
            LOG.error("config() is not properly definie");
            routingContext.response()
                    .setStatusCode(500)
                    .end();

        }

        String uuid = UUID.randomUUID().toString().toUpperCase();
        JsonObject json = new JsonObject().put(uuid, conf);
        LOG.info("UUID : {0}", json.toBuffer());
        routingContext.response()
                .putHeader("content-type", "text/html; charset=utf-8")
                .setStatusCode(200)
                .end(json.toBuffer());

    }

    @Override
    public void handle(RoutingContext routingContext) {
        String idBuild = StringUtils.EMPTY;
        HttpServerResponse response = routingContext.response();
        LOG.debug("info : " + routingContext.getBodyAsJson());

        if ((routingContext.getBodyAsJson() != null)
                && (routingContext.getBodyAsJson().getString(BUILDCALL_JSON_PARAM) != null)) {
            idBuild = routingContext.getBodyAsJson().getString(BUILDCALL_JSON_PARAM, StringUtils.EMPTY);

        } else {
            idBuild = routingContext.request().getParam("id");
        }
        LOG.info("id_build {0}", idBuild);
        if (config() == null) {
            LOG.error("config() is not properly definie");
            routingContext.response()
                    .setStatusCode(500)
                    .end();

        }

        String fullURL = this.getFullURL(idBuild);

        // 404 Not Found if log level is INFO otherwise 406
        if (StringUtils.EMPTY.equals(fullURL)) {
            responseEmpty(response);
        } else {
            //@Todo add that form configuration
            WebClientOptions options = configureWebOptions(config().getJsonObject(WEBHOOK, new JsonObject()).getJsonObject(SERVER, new JsonObject()));

            WebClient client = WebClient.create(vertx, options);
            LOG.debug("Calling : {0}", fullURL);
            //HttpRequest<JsonObject> postAbs = 
            client
                    .postAbs(fullURL)
                    .followRedirects(true)
                    .send(//(Handler<AsyncResult<HttpResponse<T>
                            ar
                            -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> resp = ar.result();
                            LOG.info("Got HTTP response with status {0} with data {1}",
                                    resp.statusCode(),
                                    resp.body().toString("ISO-8859-1"));
                            response
                                    .setStatusCode(202)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .write(resp.body())
                                    .end();
                        } else {
                            LOG.warn("Error on call : {0}", fullURL);
                            response
                                    .setStatusCode(500)
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end();
                        }
                    });
        }
    }

    private void responseEmpty(HttpServerResponse response) {
        if (LOG.isInfoEnabled()
                || LOG.isDebugEnabled()) {
            response
                    .setStatusCode(404)
                    .putHeader(BUILDCALL_JSON_PARAM, "not-found-in-current-configuration")
                    .end();

        } else {
            response
                    .setStatusCode(404)
                    .end();
        }
    }

    private WebClientOptions configureWebOptions(JsonObject webhookList) {
        WebClientOptions options = new WebClientOptions();
        if (webhookList.getJsonObject(SSL) != null) {
            if (webhookList.getJsonObject(SSL).getBoolean(TRUSTALL, Boolean.FALSE)) {
                options.setTrustAll(true);
            }
            if (StringUtils.isNotBlank(webhookList.getJsonObject(SSL).getString(PEMCERTPATH, StringUtils.EMPTY))) {
                File pemfile = new File(webhookList.getJsonObject(SSL).getString(PEMCERTPATH));
                if (pemfile.exists()) {
                    Buffer cert = vertx.fileSystem().readFileBlocking(pemfile.getAbsolutePath());
                    //options.setPemTrustOptions(new PemTrustOptions().addCertPath("/Users/csabourdin/Desktop/localminishift.crt.pem"));
                    options.setPemTrustOptions(new PemTrustOptions().addCertValue(cert));
                } else {
                    LOG.warn("File {0} does not exist", pemfile);
                }
            }

        }
        return options;
    }

    private String
            getFullURL(String idBuild
            ) {
        JsonObject webhookList = config().getJsonObject(WEBHOOK);
        String servername = "127.0.0.1";
        String port = "443";
        String namespace = "default";
        String bcuri = "buildconfigs/defaultbc/webhooks/0000000000000000/generic";

        if (webhookList
                == null) {
            LOG.error("Missing congiguration for webhook");

            return StringUtils.EMPTY;

        }

        if (webhookList.getJsonObject(SERVER) != null) {
            servername
                    = webhookList
                            .getJsonObject(SERVER)
                            .getString(NAME, servername);
            port
                    = webhookList
                            .getJsonObject(SERVER)
                            .getValue(PORT, port).toString();
            LOG.debug("serveur {0} on port {1,number,#}", servername, port);

        }

        if (webhookList
                .getJsonObject(idBuild
                ) != null) {
            namespace = webhookList
                    .getJsonObject(idBuild)
                    .getString(NAMESPACE, namespace);
            bcuri = webhookList
                    .getJsonObject(idBuild).getString(BC_URI, bcuri);
            LOG.debug("namespace {0} with URI {1}", namespace, bcuri);

        } else {
            return StringUtils.EMPTY;

        }
        String buildWebhook = "https://" + servername
                + ":" + port
                + "/oapi/v1/namespaces/"
                + namespace
                + "/" + bcuri;

        LOG.debug("buildWebhook {0} ", buildWebhook);

        return buildWebhook;

    }
    private static final String SERVER = "server";

}
