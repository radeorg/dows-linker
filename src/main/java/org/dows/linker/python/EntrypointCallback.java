package org.dows.linker.python;

public interface EntrypointCallback {
    void onSuccess(String response);

    void onError(String error);
}
