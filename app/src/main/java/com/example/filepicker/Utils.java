package com.example.filepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

public class Utils {
    public static File getFile(@NonNull final Context context, @NonNull final DocumentFile document)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            return null;
        }

        try
        {
            final List<StorageVolume> volumeList = context
                    .getSystemService(StorageManager.class)
                    .getStorageVolumes();

            if ((volumeList == null) || volumeList.isEmpty())
            {
                return null;
            }

            // There must be a better way to get the document segment
            final String documentId      = DocumentsContract.getDocumentId(document.getUri());
            final String documentSegment = documentId.substring(documentId.lastIndexOf(':') + 1);

            for (final StorageVolume volume : volumeList)
            {
                final String volumePath;

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
                {
                    final Class<?> class_StorageVolume = Class.forName("android.os.storage.StorageVolume");

                    @SuppressWarnings("JavaReflectionMemberAccess")
                    @SuppressLint("DiscouragedPrivateApi")
                    final Method method_getPath = class_StorageVolume.getDeclaredMethod("getPath");

                    volumePath = (String)method_getPath.invoke(volume);
                }
                else
                {
                    // API 30
                    volumePath = volume.getDirectory().getPath();
                }

                final File storageFile = new File(volumePath + File.separator + documentSegment);

                // Should improve with other checks, because there is the
                // remote possibility that a copy could exist in a different
                // volume (SD-card) under a matching path structure and even
                // same file name, (maybe a user's backup in the SD-card).
                // Checking for the volume Uuid could be an option but
                // as per the documentation the Uuid can be empty.

                final boolean isTarget =
                        (storageFile.exists())
                        && (storageFile.lastModified() == document.lastModified())
                        && (storageFile.length() == document.length());

                if (isTarget)
                {
                    return storageFile;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
