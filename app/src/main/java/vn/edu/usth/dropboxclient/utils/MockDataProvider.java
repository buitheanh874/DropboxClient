package vn.edu.usth.dropboxclient.utils;

import vn.edu.usth.dropboxclient.models.FileItem;
import java.util.ArrayList;
import java.util.List;

public class MockDataProvider {
    private static MockDataProvider instance;
    private List<FileItem> allFiles;

    private MockDataProvider() {
        initializeMockData();
    }

    public static synchronized MockDataProvider getInstance() {
        if (instance == null) {
            instance = new MockDataProvider();
        }
        return instance;
    }

    private void initializeMockData() {
        allFiles = new ArrayList<>();

        // Root level items
        allFiles.add(new FileItem("1", "Photos", "folder", "2025-09-20T10:30:00", 0, null));
        allFiles.add(new FileItem("2", "Documents", "folder", "2025-09-18T14:20:00", 0, null));
        allFiles.add(new FileItem("3", "resume.pdf", "pdf", "2025-09-15T08:15:00", 120345, null));
        allFiles.add(new FileItem("4", "vacation.jpg", "image", "2025-08-01T18:00:00", 2345678, null));
        allFiles.add(new FileItem("5", "project_proposal.docx", "doc", "2025-09-10T11:45:00", 456789, null));
        allFiles.add(new FileItem("6", "budget_2025.xlsx", "excel", "2025-09-05T09:30:00", 89012, null));

        // Photos folder contents
        allFiles.add(new FileItem("7", "beach_sunset.jpg", "image", "2025-08-15T19:20:00", 3456789, "1"));
        allFiles.add(new FileItem("8", "family_portrait.jpg", "image", "2025-07-22T16:10:00", 4567890, "1"));
        allFiles.add(new FileItem("9", "mountain_hike.jpg", "image", "2025-06-30T12:05:00", 2987654, "1"));
        allFiles.add(new FileItem("10", "Travel", "folder", "2025-08-10T10:00:00", 0, "1"));

        // Documents folder contents
        allFiles.add(new FileItem("11", "contract.pdf", "pdf", "2025-09-12T15:30:00", 234567, "2"));
        allFiles.add(new FileItem("12", "meeting_notes.docx", "doc", "2025-09-16T10:20:00", 45678, "2"));
        allFiles.add(new FileItem("13", "presentation.pptx", "ppt", "2025-09-14T13:45:00", 5678901, "2"));

        // Travel subfolder (inside Photos)
        allFiles.add(new FileItem("14", "paris_tower.jpg", "image", "2025-07-15T14:30:00", 3210987, "10"));
        allFiles.add(new FileItem("15", "rome_colosseum.jpg", "image", "2025-07-18T11:20:00", 3456123, "10"));
    }

    public List<FileItem> getAllFiles() {
        return new ArrayList<>(allFiles);
    }

    public List<FileItem> getFilesForParent(String parentId) {
        List<FileItem> result = new ArrayList<>();
        for (FileItem file : allFiles) {
            String fileParent = file.getParentId();
            if ((parentId == null && fileParent == null) ||
                    (parentId != null && parentId.equals(fileParent))) {
                result.add(file);
            }
        }
        return result;
    }

    public void addFile(FileItem file) {
        allFiles.add(file);
    }

    public void deleteFile(String fileId) {
        allFiles.removeIf(file -> file.getId().equals(fileId));
    }

    public FileItem getFileById(String fileId) {
        for (FileItem file : allFiles) {
            if (file.getId().equals(fileId)) {
                return file;
            }
        }
        return null;
    }
}
