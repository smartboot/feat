/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
