package com.myproject.app.common.generic;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.awfatech.strata.app.model.IMultipartProgressListener;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.CharsetUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequest extends Request<String> {

    MultipartEntityBuilder entity = MultipartEntityBuilder.create();
    HttpEntity httpentity;
    private IMultipartProgressListener mProgressListener;

    private final Response.Listener<String> mListener;
    private HashMap<String, File>mFiles;
    private HashMap<String, String> mBody;
    private long fileLength = 0L;

    public MultipartRequest(String url,
                            HashMap<String, File> mFiles,
                            HashMap<String, String> body,
                            Response.Listener<String> listener,
                            Response.ErrorListener errorListener,
                            IMultipartProgressListener progressListener) {

        super(Method.POST, url, errorListener);

        this.mListener = listener;
        this.mFiles = mFiles;
        this.mBody = body;
        this.fileLength = getFileLength();
        this.mProgressListener = progressListener;

        entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            entity.setCharset(CharsetUtils.get("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        buildMultipartEntity();
        httpentity = entity.build();
    }



//    public void addStringBody(String param, String value) {
//        mStringPart.put(param, value);
//    }

    private void buildMultipartEntity() {
        //entity.addPart(FILE_PART_NAME, new FileBody(mFilePart));

        for (Map.Entry<String,File> entry : mFiles.entrySet()) {
            if(entry.getValue() != null){
                entity.addPart(entry.getKey(), new FileBody(entry.getValue()));
            }
        }

        for (Map.Entry<String, String> entry : mBody.entrySet()) {
            if(entry.getValue() != null){
                entity.addTextBody(entry.getKey(), entry.getValue());
            }
        }
    }

    private int getFileLength(){
        int lgth = 0;
        for (Map.Entry<String,File> entry : mFiles.entrySet()) {
            if(entry.getValue() != null){
                lgth += entry.getValue().length();
            }
        }
        System.out.println("lgth = " + lgth);
        return lgth;
    }

    @Override
    public String getBodyContentType() {
        return httpentity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            //httpentity.writeTo(bos);
            httpentity.writeTo(new CountingOutputStream(bos, fileLength,
                    mProgressListener));
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {

        try{
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));

            return Response.success(jsonString,
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    public static class CountingOutputStream extends FilterOutputStream {
        private final IMultipartProgressListener progressListener;
        private long transferred;
        private long fileLength;

        public CountingOutputStream(final OutputStream out, long fileLength,
                                    final IMultipartProgressListener listener) {
            super(out);
            this.fileLength = fileLength;
            this.progressListener = listener;
            this.transferred = 0;
        }

        public void write(byte[] buffer, int offset, int length) throws IOException {
            out.write(buffer, offset, length);
            if (progressListener != null) {
                this.transferred += length;
                int progress = (int) ((transferred * 100.0f) / fileLength);
                this.progressListener.transferred(this.transferred, progress);
            }
        }

        public void write(int oneByte) throws IOException {
            out.write(oneByte);
            if (progressListener != null) {
                this.transferred++;
                int progress = (int) ((transferred * 100.0f) / fileLength);
                this.progressListener.transferred(this.transferred, progress);
            }
        }
    }
}