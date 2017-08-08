package com.sproutigy.verve.resources.props;

import java.io.IOException;

public class MemoryJSONProps extends AbstractJSONProps {
    private byte[] data;

    @Override
    protected synchronized byte[] loadRaw() throws IOException {
        return data;
    }

    @Override
    protected synchronized void saveRaw(byte[] data) throws IOException {
        this.data = data;
    }
}
