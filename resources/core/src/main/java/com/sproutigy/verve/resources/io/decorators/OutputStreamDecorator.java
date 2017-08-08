package com.sproutigy.verve.resources.io.decorators;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamDecorator extends OutputStream {
    protected OutputStream decorated;

    public OutputStreamDecorator(OutputStream decorated) {
        this.decorated = decorated;
    }

    public OutputStream getDecorated() {
        return decorated;
    }

    public void write(int b) throws IOException {
        decorated.write(b);
    }

    public void write(byte[] b) throws IOException {
        decorated.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        decorated.write(b, off, len);
    }

    public void flush() throws IOException {
        decorated.flush();
    }

    public void close() throws IOException {
        decorated.close();
    }
}
