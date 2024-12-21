package tech.smartboot.feat.core.common.io;

import java.io.IOException;
import java.util.EventListener;

public interface ReadListener extends EventListener {

    public void onDataAvailable()throws IOException;


    public void onAllDataRead() throws IOException;


    public void onError(Throwable t);

}