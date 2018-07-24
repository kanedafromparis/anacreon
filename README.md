# anacreon

This project is design to be simplier front-end to openshift, it allows to have a application dedicated to webhook, in order to not expose the api to internet.


## Build

you need a docker (DOCKER_HOST environment variable) up in order to build the image

mvn package docker:build docker:run


## API


|  URI                  |      Methods      | Description                     |  Parameter    | Example    |
|-----------------------|:-----------------:|---------------------------------|---------------------|-----------:|
| /api/1.0/rand/new     |  POST / GET       |  get a new json congiguration   |  {"namespace" : the namespace of the project to build, "bc-generic-webhook-uri": the webhook to trigger           | curl -s --header "Content-Type: application/json"   --request POST   --data '{"namespace" : "infra-anacreon-dev-ags-fr","bc-generic-webhook-uri":"buildconfigs/testing/webhooks/a6dedc8d8abbf931/generic"}' http://127.0.0.1:8080/api/1.0/rand/new     |
| /api/1.0/build/:uuid  |  POST             |  look for uuid configuration and trigger it   |  {"namespace" : the namespace of the project to build, "bc-generic-webhook-uri": the webhook to trigger           | curl -v --header "Content-Type: application/json"          --request POST           --data '{"password":"xyz"}'           http://192.168.64.7:8080/api/1.0/build/44D3C8D7-9D8A-4BDD-990C-AD76C43C4E03     |


## configuration

### anacreon-default-conf.json

|  object                                                              |      value        |  Description                                                       | Example          |
|----------------------------------------------------------------------|:-----------------:|--------------------------------------------------------------------|-----------------:|
| port                                                                 |  Integer          |  application listening port                                        |  8080            |
| webhook                                                              |  JsonObject       |                                                                    |                  |
| webhook.server.name                                                  |  String           | servername or IP of openshift                                      | api.server.com   |
| webhook.server.port                                                  |  Integer          | openshift  listening port                                          | 443              |
| webhook.server.ssl                                                   |  JsonObject       | ssl client configuration                                           |                  |
| webhook.server.ssl.trustall                                          |  Boolean          | This configuration is to skip ssl validation                       |                  |
| webhook.server.ssl.pemcertpath                                       |  String (FileURI) | This is the path to the accepted server certificats in PEM format  |                  |
| webhook.XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX                         |  JsonObject       |                                                                    |                  |
| webhook.XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.namespace               |  String           | This is the project namespece to call for buills                   | infra-anacreon-dev-ags-fr                |
| webhook.XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.bc-generic-webhook-uri  |  JsonObject       |  his is the build webhook URI                                      | "buildconfigs/testing/webhooks/cb6c50e21a5c81d4/generic" |

### logging.properties

https://docs.oracle.com/javase/10/docs/api/java/util/logging/package-summary.html


### In Openshift



```
apiVersion: v1
kind: DeploymentConfig
metadata:
  annotations:
    openshift.io/generated-by: OpenShiftWebConsole
  creationTimestamp: null
  generation: 1
  labels:
    app: proxy-builder
  name: proxy-builder
spec:
  replicas: 1
  selector:
    app: proxy-builder
    deploymentconfig: proxy-builder
  strategy:
    activeDeadlineSeconds: 21600
    recreateParams:
      timeoutSeconds: 600
    resources: {}
    type: Recreate
  template:
    metadata:
      annotations:
        openshift.io/generated-by: OpenShiftWebConsole
      creationTimestamp: null
      labels:
        app: proxy-builder
        deploymentconfig: proxy-builder
    spec:
      containers:
      - env:
        - name: JAVA_OPTIONS
          value: -Djava.security.egd=file:/dev/./urandom -XX:+UseContainerSupport
            -XshowSettings:vm -Dvertx.cacheDirBase=/tmp
        image: docker.io/kanedafromparis/anacreon:0.0.2
        imagePullPolicy: IfNotPresent
        livenessProbe:
          failureThreshold: 3
          httpGet:
            path: /liveness
            port: 8080
            scheme: HTTP
          initialDelaySeconds: 15
          periodSeconds: 10
          successThreshold: 1
          timeoutSeconds: 24
        name: proxy-builder
        ports:
        - containerPort: 8080
          protocol: TCP
        readinessProbe:
          failureThreshold: 3
          httpGet:
            path: /readiness
            port: 8080
            scheme: HTTP
          initialDelaySeconds: 15
          periodSeconds: 10
          successThreshold: 1
          timeoutSeconds: 5
        resources:
          limits:
            cpu: 500m
            memory: 512Mi
          requests:
            cpu: 500m
            memory: 512Mi
        terminationMessagePath: /dev/termination-log
        volumeMounts:
        - mountPath: /opt/anacreon/conf
          name: volume-yif13
      dnsPolicy: ClusterFirst
      imagePullSecrets:
      - name: axags-opsamb-jfrog-io
      restartPolicy: Always
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
      - configMap:
          defaultMode: 420
          name: anacreon-config
        name: volume-yif13
  test: false
  triggers:
  - imageChangeParams:
      automatic: true
      containerNames:
      - proxy-builder
      from:
        kind: ImageStreamTag
        name: anacreon:0.0.2
        namespace: aladdin-juridica-dev-ags-fr
    type: ImageChange
  - type: ConfigChange
---
apiVersion: v1
kind: ImageStream
metadata:
  annotations:
    openshift.io/image.dockerRepositoryCheck: 2018-07-20T21:45:11Z
  creationTimestamp: null
  generation: 2
  labels:
    app: anacreon
  name: anacreon
spec:
  lookupPolicy:
    local: false
  tags:
  - annotations:
      openshift.io/generated-by: OpenShiftWebConsole
      openshift.io/imported-from: docker.io/kanedafromparis/anacreon:0.0.2
    from:
      kind: DockerImage
      name: 172.30.1.1:5000/infra-anacreon-dev-ags-fr/anacreon:0.0.2
    generation: 2
    importPolicy: {}
    name: 0.0.2
    referencePolicy:
      type: Source
status:
  dockerImageRepository: ""

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: anacreon-config
data:
  anacreon-default-conf.json: |-
    {
     "port" : 8080,
     "webhook": {
        "server" : {
            "name":"192.168.64.7", ## This is the openshift server to call [manatory]
            "port":8443, //This is the port used by the openshift server to call [default: 443]
            "ssl": {
                "trustall":false, //This is to skip ssl valisation [default: false]
                "pemcertpath": "/opt/anacreon/crt/localminishift-3.9.crt.pem" //This is the path to the accepted server certificats in PEM format []
            }
         },

        "44D3C8D7-9D8A-4BDD-990C-AD76C43C4E03": { // This i
            "namespace" : "infra-anacreon-dev-ags-fr",
            "bc-generic-webhook-uri":"buildconfigs/testing/webhooks/cb6c50e21a5c81d4/generic"
        }
        }
    }  
  logging.properties: >
  
    handlers=java.util.logging.ConsoleHandler

    java.util.logging.ConsoleHandler.level=WARNING

    io.github.kandefromparis.anacreon.level=FINE
 
```