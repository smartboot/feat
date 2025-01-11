package tech.smartboot.feat.core.common.io;

import java.io.IOException;
import java.util.EventListener;

public interface ReadListener extends EventListener {

    void onDataAvailable() throws IOException;


    void onAllDataRead() throws IOException;


    void onError(Throwable t);

}