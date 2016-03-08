package com.javapoly;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class XHRHttpURLConnection extends HttpURLConnection {
  private final AtomicBoolean connectionStarted = new AtomicBoolean(false);
  private final CompletableFuture<XHRResponse> responseFuture = new CompletableFuture<>();

  XHRHttpURLConnection(final URL url) {
    super(url);
  }

  @Override public void connect() {
    if (!connectionStarted.get()) {
      connectionStarted.set(true);
      new Thread() {
        public void run() {
          final XHRResponse response= getResponse(getRequestMethod(), getURL().toString());
          responseFuture.complete(response);
        }
      }.start();
    }
  }

  @Override public String getHeaderField(final String name) {
    connect();

    try {
      return responseFuture.get().getHeaderField(name);
    } catch (final InterruptedException ie) {
      throw new RuntimeException("Interrupted while waiting for connection", ie);
    } catch (final ExecutionException ee) {
      throw new RuntimeException("Error connecting to URL", ee);
    }
  }

  @Override public InputStream getInputStream() throws IOException {
    connect();

    try {
      final byte[] responseBytes = responseFuture.get().getResponseBytes();
      return new java.io.ByteArrayInputStream(responseBytes);
    } catch (final InterruptedException ie) {
      throw new RuntimeException("Interrupted while waiting for connection", ie);
    } catch (final ExecutionException ee) {
      throw new RuntimeException("Error connecting to URL", ee);
    }
  }

  @Override public void disconnect() {
    // TODO
    System.out.println("disconnect request to: " + getURL());
  }

  @Override public boolean usingProxy() {
    return false;
  }

  @Override public final void setRequestProperty(String field, String newValue) {
    // TODO
    System.out.println("Need to set request property: " + field + ": " + newValue);
  }

  private static native XHRResponse getResponse(final String method, final String url);
}
