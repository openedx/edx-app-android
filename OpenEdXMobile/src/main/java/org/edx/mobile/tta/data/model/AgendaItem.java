package org.edx.mobile.tta.data.model;

public class AgendaItem {

    private long source;

    private long content_count;

    private String source_name;

    public long getSource() {
        return source;
    }

    public void setSource(long source) {
        this.source = source;
    }

    public long getContent_count() {
        return content_count;
    }

    public void setContent_count(long content_count) {
        this.content_count = content_count;
    }

    public String getSource_name() {
        return source_name;
    }

    public void setSource_name(String source_name) {
        this.source_name = source_name;
    }
}
