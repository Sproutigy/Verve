package com.sproutigy.verve.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class URLDataAccess extends AbstractDataAccess {
    private URL url;

    public URLDataAccess(URL url) {
        this.url = url;
    }

    @Override
    public InputStream input() throws IOException {
        return url.openStream();
    }

    @Override
    public OutputStream output() throws IOException {
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        return connection.getOutputStream();
    }

    @Override
    public void close(boolean commit) throws Exception {

    }

}
