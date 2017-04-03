package com.sproutigy.verve.webserver.handlers;

import com.sproutigy.verve.resources.FileResource;
import com.sproutigy.verve.resources.Resource;
import com.sproutigy.verve.resources.fs.ResourceLocalFile;
import com.sproutigy.verve.webserver.HttpHandler;
import com.sproutigy.verve.webserver.HttpRequestContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class ServeResourceHttpHandler implements HttpHandler {

    @Getter
    private String prefix;

    @Getter
    private Resource root;

    @Getter @Setter
    private Collection<String> defaultFiles = Arrays.asList("index.html", "index.htm");

    public ServeResourceHttpHandler(String prefix, String localPath) throws IOException {
        this(prefix, new FileResource(localPath));
    }

    public ServeResourceHttpHandler(String prefix, File root) throws IOException {
        this(prefix, new FileResource(root));
    }

    public ServeResourceHttpHandler(String prefix, Resource root) throws IOException {
        this.prefix = prefix;
        this.root = root; //new SimpleResourceTree(root).getRoot();
    }

    public ServeResourceHttpHandler(String localPath) throws IOException {
        this("/", localPath);
    }

    public ServeResourceHttpHandler(File root) throws IOException {
        this("/", root);
    }

    public ServeResourceHttpHandler(Resource root) throws IOException {
        this("/", root);
    }

    @Override
    public Object handle(HttpRequestContext ctx) throws Exception {
        if (!ctx.getResponse().isFinalized()) {
            String p = ctx.getRequest().getPath();
            if (p.startsWith(prefix)) {
                p = p.substring(prefix.length());

                Resource requestedResource = root.resolve(p);
                Resource targetResource = requestedResource;

                if (requestedResource.isContainer()) {
                    if (!p.endsWith("/")) {
                        return new RedirectHttpHandler(x -> ctx.getRequest().getPath() + "/").handle(ctx);
                    } else {
                        boolean found = false;
                        for (String defaultFile : defaultFiles) {
                            Resource defaultFileCheck = requestedResource.child(defaultFile);
                            if (defaultFileCheck.exists()) {
                                targetResource = defaultFileCheck;
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            ctx.getResponse().status(HttpResponseStatus.FORBIDDEN).end();
                            return null;
                        }
                    }
                }

                ResourceLocalFile resourceLocalFile = new ResourceLocalFile(targetResource);
                File f = resourceLocalFile.provideLocalFile();

                if (filterFile(f)) {
                    ctx.getResponse().sendFile(f.getPath()).then(x -> { resourceLocalFile.close(); return null; });
                }
            }
        }

        return null;
    }

    protected boolean filterFile(File f) throws Exception {
        return true;
    }
}
