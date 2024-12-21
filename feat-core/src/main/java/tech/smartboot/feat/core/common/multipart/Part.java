
package tech.smartboot.feat.core.common.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


public interface Part {

    public InputStream getInputStream() throws IOException;

    public String getContentType();


    public String getName();


    public String getSubmittedFileName();


    public long getSize();


    public void write(String fileName) throws IOException;


    public void delete() throws IOException;


    public String getHeader(String name);


    public Collection<String> getHeaders(String name);


    public Collection<String> getHeaderNames();

}
