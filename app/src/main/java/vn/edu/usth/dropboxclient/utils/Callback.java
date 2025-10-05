package vn.edu.usth.dropboxclient.utils;

public interface Callback<T> {
    void onSuccess(T data);
    void onError(String message);
}
