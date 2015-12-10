package com.google.android.gms.drive.sample.quickstart;

/**
 * Created by Hamot on 11/23/2015.
 */
public class CustomFile {

    private byte[] content;
    private String ext;

    public CustomFile(byte[] mContent, String mExt) {
        content = mContent;
        ext = mExt;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }
}
