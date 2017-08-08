package com.sproutigy.verve.resources.io.decorators;

import java.io.IOException;
import java.io.Writer;

public class WriterDecorator extends Writer {
    private Writer decorated;

    public WriterDecorator(Writer decorated) {
        this.decorated = decorated;
    }

    public Writer getDecorated() {
        return decorated;
    }

    @Override
    public void write(int c) throws IOException {
        decorated.write(c);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        decorated.write(cbuf);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        decorated.write(cbuf, off, len);
    }

    @Override
    public void write(String str) throws IOException {
        decorated.write(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        decorated.write(str, off, len);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        return decorated.append(csq);
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        return decorated.append(csq, start, end);
    }

    @Override
    public Writer append(char c) throws IOException {
        return decorated.append(c);
    }

    @Override
    public void flush() throws IOException {
        decorated.flush();
    }

    @Override
    public void close() throws IOException {
        decorated.close();
    }
}
