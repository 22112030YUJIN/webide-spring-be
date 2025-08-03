package com.withquery.webide_spring_be.domain.file.service;

import com.withquery.webide_spring_be.domain.file.dto.FileContentResponse;
import com.withquery.webide_spring_be.domain.file.dto.FileContentUpdateRequest;
import com.withquery.webide_spring_be.domain.file.dto.FileCreateRequest;
import com.withquery.webide_spring_be.domain.file.dto.FileResponse;
import com.withquery.webide_spring_be.domain.file.dto.FileTreeNode;
import com.withquery.webide_spring_be.domain.file.dto.FileUpdateRequest;

public interface FileService {

	void createRootDirectory(Long projectId, String projectName);

	FileTreeNode getFileTree(Long projectId);

	FileResponse createFile(Long projectId, FileCreateRequest request);

	FileResponse updateFile(Long projectId, FileUpdateRequest request);

	void deleteFile(Long projectId, Long fileId);

	FileContentResponse getFileContent(Long projectId, Long fileId);

	FileResponse updateFileContent(Long projectId, FileContentUpdateRequest request);

	void deleteProjectFilesRecursively(Long projectId);
}
