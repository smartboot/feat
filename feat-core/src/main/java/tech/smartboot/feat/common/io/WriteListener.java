package tech.smartboot.feat.common.io;

import java.io.IOException;
import java.util.EventListener;

public interface WriteListener extends EventListener {


    public void onWritePossible() throws IOException;


    public void onError(final Throwable t);

}