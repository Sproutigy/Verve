package com.sproutigy.verve.resources.io.decorators;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class ReaderDecorator extends Reader {
    private Reader decorated;

    public ReaderDecorator(Reader decorated) {
        this.decorated = decorated;
    }

    public Reader getDecorated() {
        return decorated;
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        return decorated.read(target);
    }

    @Override
    public int read() throws IOException {
        return decorated.read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return decorated.read(cbuf);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return decorated.read(cbuf, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return decorated.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
        return decorated.ready();
    }

    @Override
    public boolean markSupported() {
        return decorated.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        decorated.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        decorated.reset();
    }

    @Override
    public void close() throws IOException {
        decorated.close();
    }
}
