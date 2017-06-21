package triangle.triangleapp.persistence.stream.impl;

import android.support.annotation.NonNull;
import android.util.Log;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.spongycastle.openssl.jcajce.JcaPEMWriter;

import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;

import triangle.triangleapp.helpers.ConfigHelper;
import triangle.triangleapp.helpers.IntegrityHelper;
import triangle.triangleapp.persistence.stream.StreamAdapter;
import triangle.triangleapp.persistence.ConnectionCallback;

/**
 * Created by Kevin Ly on 6/15/2017.
 */

public class WebSocketStream implements StreamAdapter {

    private static final String TAG = "WebSocket/sendStream";

    private static final String URL = ConfigHelper.getInstance().get(ConfigHelper.KEY_STREAM_DESTINATION_ADDRESS);
    private static final String PROTOCOL = ConfigHelper.getInstance().get(ConfigHelper.KEY_WEBSOCKET_PROTOCOL);
    private WebSocket mWebSocket;
    private boolean mIsConnected;

    /**
     * Determines if the WebSocket is connected.
     *
     * @return True if connected else false
     */
    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public void sendPublicKey(@NonNull PublicKey publicKey) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(stringWriter);
        try {
            writer.writeObject(publicKey);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String pubKey = stringWriter.toString();
        mWebSocket.send("PUBKEY:" + pubKey);
    }

    @Override
    public void connect(@NonNull final ConnectionCallback callback) {
        AsyncHttpClient.getDefaultInstance()
                .websocket(URL, PROTOCOL, new AsyncHttpClient.WebSocketConnectCallback() {
                    @Override
                    public void onCompleted(Exception ex, WebSocket webSocket) {
                        if (ex == null) {
                            mIsConnected = true;
                            mWebSocket = webSocket;
                            callback.onConnected();
                        } else {
                            callback.onError(ex);
                        }
                    }
                });
    }

    /**
     * Sends a byte array that is signed with the privateKey
     *
     * @param fileInBytes file in bytes
     * @param privateKey  The private key to use for signing
     */
    @Override
    public void sendFile(@NonNull byte[] fileInBytes, @NonNull PrivateKey privateKey) {
        try {
            if (mIsConnected) {
                String hash = IntegrityHelper.sign(fileInBytes, privateKey);
                mWebSocket.send("HASH:" + hash);
                mWebSocket.send(fileInBytes);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error while sending stream.", ex);
        }
    }

    @Override
    public void sendText(@NonNull String text) {
        try {
            if (mIsConnected) {
                mWebSocket.send(text);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error while sending text.", ex);
        }
    }
}



