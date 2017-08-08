package com.sproutigy.verve.resources.io.decorators;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamDecorator extends InputStream {

    protected InputStream decorated;

    public InputStreamDecorator(InputStream decorated) {
        this.decorated = decorated;
    }

    public InputStream getDecorated() {
        return decorated;
    }

    @Override
    public int read() throws IOException {
        return decorated.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return decorated.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return decorated.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return decorated.skip(n);
    }

    @Override
    public int available() throws IOException {
        return decorated.available();
    }

    @Override
    public void close() throws IOException {
        decorated.close();
    }

    @Override
    public void mark(int readlimit) {
        decorated.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        decorated.reset();
    }

    @Override
    public boolean markSupported() {
        return decorated.markSupported();
    }
}
