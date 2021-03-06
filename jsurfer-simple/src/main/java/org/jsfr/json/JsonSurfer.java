/*
 * The MIT License
 *
 * Copyright (c) 2015 WANG Lingsong
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jsfr.json;

import org.jsfr.json.SurfingContext.Builder;
import org.jsfr.json.path.JsonPath;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;

import static org.jsfr.json.SurfingContext.builder;
import static org.jsfr.json.compiler.JsonPathCompiler.compile;


/**
 * JsonSurfer traverses the whole json DOM tree, compare the path of each node with registered JsonPath
 * and return the matched value immediately. JsonSurfer is fully streaming which means that it doesn't construct the DOM tree in memory.
 */
public class JsonSurfer {

    private JsonProvider jsonProvider;
    private JsonParserAdapter jsonParserAdapter;
    private ErrorHandlingStrategy errorHandlingStrategy;

    /**
     * @return New JsonSurfer using JsonSimple parser and provider. JsonSimple dependency is included by default.
     */
    public static JsonSurfer simple() {
        return new JsonSurfer(new JsonSimpleParser(), new JsonSimpleProvider());
    }

    /**
     * @return New JsonSurfer using Gson parser and provider. You need to explicitly declare gson dependency.
     */
    public static JsonSurfer gson() {
        return new JsonSurfer(new GsonParser(), new GsonProvider());
    }

    /**
     * @return New JsonSurfer using Jackson parser and provider. You need to explicitly declare Jackson dependency.
     */
    public static JsonSurfer jackson() {
        return new JsonSurfer(new JacksonParser(), new JacksonProvider());
    }

    /**
     * @param jsonParserAdapter
     * @param jsonProvider
     */
    public JsonSurfer(JsonParserAdapter jsonParserAdapter, JsonProvider jsonProvider) {
        this.jsonParserAdapter = jsonParserAdapter;
        this.jsonProvider = jsonProvider;
        this.errorHandlingStrategy = new DefaultErrorHandlingStrategy();
    }

    /**
     * @param jsonParserAdapter
     * @param jsonProvider
     * @param errorHandlingStrategy
     */
    public JsonSurfer(JsonParserAdapter jsonParserAdapter, JsonProvider jsonProvider, ErrorHandlingStrategy errorHandlingStrategy) {
        this.jsonProvider = jsonProvider;
        this.jsonParserAdapter = jsonParserAdapter;
        this.errorHandlingStrategy = errorHandlingStrategy;
    }

    public void surf(String json, SurfingContext context) {
        surf(new StringReader(json), context);
    }

    /**
     * @param reader  Json source
     * @param context SurfingContext that holds JsonPath binding
     */
    public void surf(Reader reader, SurfingContext context) {
        ensureSetting(context);
        jsonParserAdapter.parse(reader, context);
    }

    public Collection<Object> collectAll(String json, JsonPath... paths) {
        return collectAll(new StringReader(json), paths);
    }

    /**
     * Collect all matched value into a collection
     *
     * @param reader
     * @param paths  JsonPath
     * @return All matched value
     */
    public Collection<Object> collectAll(Reader reader, JsonPath... paths) {
        return collectAll(reader, Object.class, paths);
    }

    public <T> Collection<T> collectAll(String json, Class<T> tClass, JsonPath... paths) {
        return collectAll(new StringReader(json), tClass, paths);
    }

    /**
     * Collect all matched value into a collection
     *
     * @param reader
     * @param tClass
     * @param paths
     * @param <T>
     * @return
     */
    public <T> Collection<T> collectAll(Reader reader, Class<T> tClass, JsonPath... paths) {
        CollectAllListener<T> listener = new CollectAllListener<T>(jsonProvider, tClass);
        Builder builder = builder();
        for (JsonPath jsonPath : paths) {
            builder.bind(jsonPath, listener);
        }
        surf(reader, builder.build());
        return listener.getCollection();
    }

    public <T> Collection<T> collectAll(String json, Class<T> tClass, String... paths) {
        return collectAll(new StringReader(json), tClass, paths);
    }

    /**
     * Collect all matched value into a collection
     *
     * @param reader
     * @param tClass
     * @param paths
     * @param <T>
     * @return
     */
    public <T> Collection<T> collectAll(Reader reader, Class<T> tClass, String... paths) {
        return collectAll(reader, tClass, compile(paths));
    }

    public Collection<Object> collectAll(String json, String... paths) {
        return collectAll(new StringReader(json), paths);
    }

    /**
     * Collect all matched value into a collection
     *
     * @param reader
     * @param paths
     * @return
     */
    public Collection<Object> collectAll(Reader reader, String... paths) {
        return collectAll(reader, Object.class, paths);
    }

    public Object collectOne(String json, JsonPath... paths) {
        return collectOne(new StringReader(json), paths);
    }

    /**
     * Collect the first matched value and stop parsing immediately
     *
     * @param reader
     * @param paths
     * @return Matched value
     */
    public Object collectOne(Reader reader, JsonPath... paths) {
        return collectOne(reader, Object.class, paths);
    }

    public <T> T collectOne(String json, Class<T> tClass, JsonPath... paths) {
        return collectOne(new StringReader(json), tClass, paths);
    }

    /**
     * Collect the first matched value and stop parsing immediately
     *
     * @param reader
     * @param tClass
     * @param paths
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T collectOne(Reader reader, Class<T> tClass, JsonPath... paths) {
        CollectOneListener listener = new CollectOneListener();
        Builder builder = builder().skipOverlappedPath();
        for (JsonPath jsonPath : paths) {
            builder.bind(jsonPath, listener);
        }
        surf(reader, builder.build());
        Object value = listener.getValue();
        return tClass.cast(jsonProvider.cast(value, tClass));
    }

    public <T> T collectOne(String json, Class<T> tClass, String... paths) {
        return collectOne(new StringReader(json), tClass, paths);
    }

    /**
     * Collect the first matched value and stop parsing immediately
     *
     * @param reader
     * @param tClass
     * @param paths
     * @param <T>
     * @return
     */
    public <T> T collectOne(Reader reader, Class<T> tClass, String... paths) {
        return collectOne(reader, tClass, compile(paths));
    }

    public Object collectOne(String json, String... paths) {
        return collectOne(new StringReader(json), paths);
    }

    /**
     * Collect the first matched value and stop parsing immediately
     *
     * @param reader
     * @param paths
     * @return
     */
    public Object collectOne(Reader reader, String... paths) {
        return collectOne(reader, Object.class, paths);
    }

    private void ensureSetting(SurfingContext context) {
        if (context.getJsonProvider() == null) {
            context.setJsonProvider(jsonProvider);
        }
        if (context.getErrorHandlingStrategy() == null) {
            context.setErrorHandlingStrategy(errorHandlingStrategy);
        }
    }

}
