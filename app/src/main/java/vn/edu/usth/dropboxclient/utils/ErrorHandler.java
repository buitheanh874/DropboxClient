package vn.edu.usth.dropboxclient.utils;

import android.content.Context;

import com.dropbox.core.DbxException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ErrorHandler {

    public static void showErrorDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showErrorDialog(Context context, Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "An unknown error occurred";
        }
        String title = "Error";
        if (e instanceof DbxException) {
            title = "Dropbox Error";
        }
        showErrorDialog(context, title, errorMessage);
    }

    public static void showSuccessDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showConfirmDialog(Context context, String title, String message,
                                         String positiveText, String negativeText,
                                         Runnable onPositive, Runnable onNegative) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> {
                    if (onPositive != null) {
                        onPositive.run();
                    }
                })
                .setNegativeButton(negativeText, (dialog, which) -> {
                    if (onNegative != null) {
                        onNegative.run();
                    }
                })
                .show();
    }
}