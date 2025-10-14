package vn.edu.usth.dropboxclient.utils;

import android.content.Context;

import com.dropbox.core.DbxException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Utility class cho error handling với MaterialAlertDialog
 */
public class ErrorHandler {

    /**
     * Hiển thị error dialog với title và message
     */
    public static void showErrorDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Hiển thị error dialog từ Exception
     */
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

    /**
     * Hiển thị success dialog
     */
    public static void showSuccessDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Hiển thị confirm dialog
     */
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