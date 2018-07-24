/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.kandefromparis.anacreon;

/**
 * This Class is only a class that keep track of the URL
 *
 * @author csabourdin
 */
public enum ConfAPICall {
    ASSETX("/assets/*"),
    API_1_0_BUILD("/api/1.0/build/:id"),
    RANDOM_UUID("/api/1.0/rand/:id"),    
    LIVENESS("/liveness"),
    READINESS("/readiness");

    // Cheers API
    private final String url;

    ConfAPICall(String url) {
        this.url = url;
    }

    public String getURL() {
        return this.url;
    }

    public String build(String s) {
        return this.url + s;
    }
}
